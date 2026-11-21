# PWA and notification verification matrix

This matrix is the acceptance checklist for a configured environment. It has not been run on physical phones as part of this code change.

| Area | Scenario | Expected result |
| --- | --- | --- |
| Install | Open app on HTTPS or `localhost`; install from the browser | App uses the EduCenter name, icon, colors and standalone display |
| Offline | Open a page, then disconnect the network and navigate | Offline fallback appears; private page contents are not served from cache |
| Permission | Open device settings without clicking enable | No browser permission prompt appears |
| Subscription | Click enable after signing in to two or three accounts in one browser | Each account gets its own active link to the same physical browser subscription |
| Ownership | Remove one account's link; try to remove another user's link ID or submit mismatched endpoint keys | Only the current account's link is removed; another account still receives; cross-account deletion and mismatched keys are rejected |
| Push | Send a parent/teacher/admin message to a signed-up offline device | A generic push appears by default and opens the relevant conversation |
| Preview | Enable notifications and send a grade, report or homework event | Lock-screen title/body remain generic and contain no student name, score or answer |
| Quiet hours | Set a quiet window and create a push event in that window | Notification is stored immediately; push is scheduled for the configured quiet-end time |
| Retry | Make the provider request fail temporarily | Outbox retries with backoff; a permanent 404/410 disables the expired device |
| Message compatibility | Poll `GET /api/notifications` while there are unread messages | Existing unread summary shape and URLs remain available |
| Business badge | Create and read a business notification | Business badge updates separately from message unread count; supported app badges include both counts |
| Realtime chat | Open the same conversation in two accounts and send a message | Connected members receive it over STOMP; unauthorized users cannot subscribe; HTTP polling remains fallback |
| Active chat | Keep the target conversation open while another member sends a message | The chat updates in real time and its push is suppressed while that recipient is viewing the thread |
| Class groups | Enroll/remove a student or reassign a teacher | The class group membership reflects active enrollment and current assigned teachers after synchronization |
| Voice | Record a short message or attach an audio file | Audio is stored as a chat attachment and can be played by conversation members |
| Mentions | Mention a group member; submit an ID outside the conversation | Member is notified; non-members are rejected |
| Important chat | Send an important message as teacher/admin and as parent | Teacher/admin messages show the flag; a parent attempt is rejected |
| Attendance/report | Save attendance and publish session/monthly reports | Parents receive linked business notifications for their own children |
| Tuition/schedule/material | Create a fee, record payment, change a schedule, publish class material | Parents in the affected classes receive the matching notification |
| Broadcast | Send to all/one role immediately and schedule a future broadcast | Correct recipients see one notification; scheduled broadcast is not visible before its send time |
| Homework | Assign, upload, review, submit late and submit as another parent's child | Assigned-class users are notified; late submissions are retained; unrelated parents are denied |
| iOS | Add the site to the Home Screen on iOS/iPadOS 16.4+ and enable push | Permission and push are available; browser-only tabs are not treated as installed PWA push targets |
