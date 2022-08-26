

|Name:|Long option: |Short option:|Description:|
| :- | :- | :- | :- |
|CommandFile|-commandFile|-c|Location of the csv command file|
|Token|-token|-t|Slack API token|

Token can be created by following the “Slack\_Installation\_Guide.pdf”

CommandFile is CSV format, see below for possible commands.






|Command|Year|Section|Last Arg|
| :- | :- | :- | :- |
|CREATECHANNEL|year|section|public/private|
|ADDSTUDENT|year|section|student\_email1, student\_email2…|
|REMOVESTUDENT|year|section|student\_email1, student\_email2…|
|MOVESTUDENT|year|section|student\_email1, student\_email2…|
|ARCHIVECHANNEL|year|section||
|UNARCHIVECHANNEL|year|section||

Section has 3 possibilities, “AI”, “DS”, “”. If blank, will deal with the channel for the entire year instead of just that section. I.e. year: “2024”, section: “DS” will use channel: class-of-2024-ds. But, year: “2024”, section: ”” will use the channel class-of-2024.

If a student is in multiple sections or years using MOVESTUDENT will result in the student being removed from 1-all of the other Channels, and added to the passed channel. MOVESTUDENT is not an atomic operation even when removing from and adding to the same Channel.

Examples: 

*CREATECHANNEL,2024,DS,private* will create a private channel class-of-2024-ds

*ADDSTUDENT,2024,DS,email1@gmail.com,email2@gmail.com,email99@gmail.com* will add the users corresponding to all the emails to class-of-2024-ds

*MOVESTUDENT,2024,DS,email1@gmail.com,email2@gmail.com* removes the students from any specific section channel they’re in and adds them to class-of-2024-ds. For example, if *email1@gmail.com* is in the channels: class-of-2023-ai, class-of-2023 when the command is called, they will be removed from just class-of-2023-ai and added to class-of-2024-ds. So, at the end their channel list will look like: class-of-2024-ds, class-of-2023.

*MOVESTUDENT,2024,,email1@gmail.com,email2@gmail.com* will remove the students from whatever grade channel they’re currently in. I.e. if email1@gmail.com is in class-of-2024-ds, class-of-2023, at the end of the operation they will be in: class-of-2024-ds, class-of-2024.
