# Wireframe decisions (2026-11-19)

Source: artifact "Web Chat Wireframe MVP" (6 screens, light and dark). Owner decisions below.

## Decisions

1. **In-conversation search ("Tìm trong hội thoại") is kept.** Not in the roadmap yet (roadmap only has M7-01, search for friends and groups). Product agent must add a story and assign an ID before build.
2. **Typing indicator ("đang soạn tin") is in MVP.** Product agent must add a story and assign an ID.
3. **Login lockout is 15 minutes** after 5 failed attempts (matches the analysis document). The Login screen text was updated to "Thử lại sau 15 phút."
4. **Product name is "Web Chat".** The Login screen placeholder was replaced.
5. **Registration, OTP verification and forgot-password are already built in the repo.** They are not part of this wireframe. Do not redesign them unless the owner asks.
6. **Mobile layout is out of scope for now.** The wireframe is desktop only (1280 px).

## Still open

- Privacy and notification tabs in Settings have no content yet. The "only friends can message" setting (M16-03) needs a design.
- Empty, error and loading states are not in the wireframe.
- Avatar upload limit is shown as 5 MB on the design. Confirm this number.

## Notes for agents

- The `frontend` agent must map the liquid glass tokens (glass, glass2, border, shadow, blur) into the Tailwind theme. Do not use shadcn defaults for these.
- The `product` agent must add stories for decisions 1 and 2 before any build starts.
