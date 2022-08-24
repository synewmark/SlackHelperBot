package backendhandlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.auth.AuthTestResponse;
import com.slack.api.methods.response.conversations.ConversationsArchiveResponse;
import com.slack.api.methods.response.conversations.ConversationsCreateResponse;
import com.slack.api.methods.response.conversations.ConversationsInviteResponse;
import com.slack.api.methods.response.conversations.ConversationsKickResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.conversations.ConversationsMembersResponse;
import com.slack.api.methods.response.conversations.ConversationsUnarchiveResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.methods.response.users.UsersListResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;
import com.slack.api.model.User;

public class SlackHandler {
	private static Slack slack = Slack.getInstance();
	private MethodsClient methods;
	private final String token;

	List<String> scopes;

	private final User self;

	private Map<String, String> nameToConversationMap = new HashMap<>();
	private Map<String, Set<String>> completeUserMap = new HashMap<>();
	private Map<String, String> emailToUserMap = new HashMap<>();

	private Set<String> generalChannels = new HashSet<>();
	private Set<String> gradeChannels = new HashSet<>();
	private Set<String> trackChannels = new HashSet<>();

	private Set<String> archivedChannels = new HashSet<>();

	public SlackHandler(String token) {
		this.token = token;
		this.methods = slack.methods(token);
		try {
			String userID = getUserIDAndSetHeaders();
			checkScopes();
			this.self = getUserFromId(userID);
			for (Conversation channel : getListOfChannels()) {
				String channelName = channel.getNameNormalized();
				if (channelName == null) {
					continue;
				}
				nameToConversationMap.put(channelName, channel.getId());
				addChannelToSets(channelName, channel);
				completeUserMap.put(channel.getId(), new HashSet<>(getUsersInChannel(channel.getId())));
			}

			for (User user : getAllUsers()) {
				String email = user.getProfile().getEmail();
				if (email != null) {
					emailToUserMap.put(email, user.getId());
				}
			}
		} catch (IOException | SlackApiException e) {
			throw new IllegalStateException("Error initializing");
		}
	}

	public String addUserToChannel(String userEmail, String channelName) throws IOException, SlackApiException {
		checkEmail(userEmail);
		checkChannel(channelName);
		String userID = emailToUserMap.get(userEmail);
		String channelID = nameToConversationMap.get(channelName);
		ConversationsInviteResponse response = methods
				.conversationsInvite(r -> r.token(token).channel(channelID).users(List.of(userID)));
		if (response.isOk()) {
			completeUserMap.get(channelID).add(userID);
		}
		return response.getError();
	}

	public String removeUserFromChannel(String userEmail, String channelName) throws IOException, SlackApiException {
		checkChannel(channelName);
		String channelID = nameToConversationMap.get(channelName);
		return removeUserFromChannelUsingID(userEmail, channelID);
	}

	private String removeUserFromChannelUsingID(String userEmail, String channelID)
			throws IOException, SlackApiException {
		checkEmail(userEmail);
		String userID = emailToUserMap.get(userEmail);
		ConversationsKickResponse response = methods
				.conversationsKick(r -> r.token(token).channel(channelID).user(userID));
		if (response.isOk()) {
			completeUserMap.get(nameToConversationMap.get(userEmail)).remove(userEmail);
		}
		return response.getError();
	}

	public String moveUserToChannel(String userEmail, String channelName) throws IOException, SlackApiException {
		checkEmail(userEmail);
		checkChannel(channelName);

		final boolean gradeOrTrack;
		if (isGradeChannel(channelName)) {
			gradeOrTrack = true;
		} else if (isTrackChannel(channelName)) {
			gradeOrTrack = false;
		} else {
			throw new IllegalArgumentException("Cannot use move command with non grade/track channel: " + channelName);
		}
		String removalResponse = removeFromAnyChannel(userEmail, gradeOrTrack);
		if (removalResponse != null) {
			return removalResponse;
		}
		return addUserToChannel(userEmail, channelName);
	}

	private String removeFromAnyChannel(String userEmail, boolean gradeOrTrack) throws IOException, SlackApiException {
		var setToSearch = gradeOrTrack ? gradeChannels : trackChannels;
		for (String channelID : setToSearch) {
			if (completeUserMap.get(channelID).contains(userEmail)) {
				return removeUserFromChannelUsingID(userEmail, channelID);
			}
		}
		return null;
	}

	public String createChannel(String channelName, boolean makePrivate) throws IOException, SlackApiException {
		ConversationsCreateResponse response = methods
				.conversationsCreate(r -> r.token(token).name(channelName).isPrivate(makePrivate));
		if (response.getChannel() != null) {
			nameToConversationMap.put(channelName, response.getChannel().getId());
			Set<String> selfSet = new HashSet<>();
			selfSet.add(self.getId());
			completeUserMap.put(response.getChannel().getId(), selfSet);
			addChannelToSets(channelName, response.getChannel());
		}
		return response.getError();
	}

	public String archiveChannel(String channelName) throws IOException, SlackApiException {
		checkChannel(channelName);
		ConversationsArchiveResponse response = methods.conversationsArchive(r -> r.token(token).channel(channelName));
		if (response.isOk()) {
			archivedChannels.add(nameToConversationMap.get(channelName));
		}
		return response.getError();
	}

	public String unarchiveChannel(String channelName) throws IOException, SlackApiException {
		checkChannel(channelName);
		ConversationsUnarchiveResponse response = methods
				.conversationsUnarchive(r -> r.token(token).channel(channelName));
		if (response.isOk()) {
			archivedChannels.remove(nameToConversationMap.get(channelName));
		}
		return response.getError();
	}

	public Set<String> getAllUserEmails() {
		return Collections.unmodifiableSet(emailToUserMap.keySet());
	}

	private void checkEmail(String userEmail) {
		if (!emailToUserMap.containsKey(userEmail)) {
			throw new IllegalArgumentException("User: " + userEmail + " does not exist!");
		}
	}

	private void checkChannel(String channelName) {
		if (!nameToConversationMap.containsKey(channelName)) {
			throw new IllegalArgumentException("Channel: " + channelName + " does not exist!");
		}
	}

	private List<Conversation> getListOfChannels() throws IOException, SlackApiException {
		ConversationsListResponse result = methods.conversationsList(
				r -> r.token(token).types(List.of(ConversationType.PUBLIC_CHANNEL, ConversationType.PRIVATE_CHANNEL)));
		return result.getChannels() != null ? result.getChannels() : new ArrayList<Conversation>();
	}

	private List<User> getAllUsers() throws IOException, SlackApiException {
		UsersListResponse result = methods.usersList(r -> r.token(token));
		return result.getMembers();
	}

	private List<String> getUsersInChannel(String channelID) throws IOException, SlackApiException {
		ConversationsMembersResponse result = methods.conversationsMembers((r) -> r.token(token).channel(channelID));
		return result.getMembers();
	}

	private User getUserFromId(String userID) throws IOException, SlackApiException {
		UsersInfoResponse result = methods.usersInfo((r) -> r.token(token).user(userID));
		return result.getUser();
	}

	private void addChannelToSets(String name, Conversation channel) {
		if (isGradeChannel(name)) {
			gradeChannels.add(channel.getId());
		} else if (isTrackChannel(name)) {
			trackChannels.add(channel.getId());
		} else {
			generalChannels.add(channel.getId());
		}
		if (channel.isArchived()) {
			archivedChannels.add(channel.getId());
		}
	}

	private static boolean isGradeChannel(String name) {
		return name.matches("\\b(class_of_)\\b\\d{4}");
	}

	private static boolean isTrackChannel(String name) {
		return name.matches("\\b(class_of_)\\b\\d{4}_\\b(ai|ds)\\b");
	}

	private String getUserIDAndSetHeaders() throws IOException, SlackApiException {
		AuthTestResponse response = methods.authTest(r -> r.token(token));
		if (!response.isOk()) {
			throw new IllegalArgumentException("Token not valid: " + response.getError());
		}
		if (response.getBotId() != null) {
			throw new IllegalArgumentException("Must use user token, not bot token");
		}
		String scopeBlock = response.getHttpResponseHeaders().get("x-oauth-scopes").get(0);
		List<String> scopes = List.of(scopeBlock.split(","));
		this.scopes = scopes;
		return response.getUserId();
	}

	private void checkScopes() {
		var requiredScopes = new TreeSet<>(
				List.of("channels:write", "groups:write", "users.profile:read", "users:read", "users:read.email"));
		requiredScopes.removeAll(scopes);
		if (!requiredScopes.isEmpty()) {
			throw new IllegalArgumentException("Token is missing the following scopes: " + requiredScopes.toString());
		}
	}

	// specialEquals implementation is done statefully and does not fulfill standard
	// equals contract
	public boolean specialEquals(SlackHandler other) {
		return vertifyEquals(archivedChannels, other.archivedChannels)
				&& vertifyEquals(completeUserMap, other.completeUserMap)
				&& vertifyEquals(emailToUserMap, other.emailToUserMap)
				&& vertifyEquals(generalChannels, other.generalChannels)
				&& vertifyEquals(gradeChannels, other.gradeChannels)
				&& vertifyEquals(nameToConversationMap, other.nameToConversationMap);
	}

	private boolean vertifyEquals(Object a, Object b) {
		boolean returnVal = Objects.equals(a, b);
		if (!returnVal) {
			System.out.println(String.format("Expected: %s/n got: %s", a.toString(), b.toString()));
		}
		return returnVal;
	}

}
