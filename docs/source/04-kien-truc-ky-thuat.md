# Web Chat – Kiến trúc & Tech stack MVP (v0.1)

_Người lập: Claude (Senior PM) · Ngày: 2026-09-26 · Bối cảnh: server Java + MongoDB (Kam đang dựng), ~100 người dùng, đội 3 người bán thời gian_

Nguyên tắc: **đơn giản nhất có thể để kịp cuối tháng 12**, nhưng không chặn đường mở rộng ở Phase 2 (gọi thoại/video).

## 1. Tech stack đề xuất

| Tầng | Lựa chọn | Lý do |
|---|---|---|
| Backend | **Java 21 + Spring Boot 3** (giả định Kam dùng Spring; nếu khác thì giữ framework hiện tại) | Hệ sinh thái đầy đủ cho REST, WebSocket, bảo mật |
| Xác thực | Spring Security + **OAuth2 Client (Google)** + JWT (access 15 phút, refresh 30 ngày) | Server đã có email + Google |
| Realtime | **Spring WebSocket + STOMP** (broker in-memory) | Có sẵn trong Spring, client STOMP.js ổn định; 100 người thì 1 instance là đủ |
| Database | **MongoDB** (Atlas M0/M10 hoặc tự host) | Hợp với dữ liệu tin nhắn dạng document |
| Lưu file | Object storage tương thích S3 (**Cloudflare R2** hoặc MinIO), upload qua **presigned URL** | Không đẩy file qua server Java; R2 miễn phí băng thông ra |
| Web push | Service Worker + thư viện `web-push` Java (VAPID) | Thông báo khi đóng tab |
| Frontend | **React + TypeScript + Vite**, STOMP.js, TanStack Query | Phổ biến, dễ tuyển/nhờ giúp, nhiều thư viện UI chat |
| UI (Kam chốt 26/09) | **shadcn/ui** (Radix + Tailwind CSS), Motion (hiệu ứng), react-virtuoso (danh sách tin nhắn), lucide-react (icon), Frimousse (emoji) | Tuỳ biến toàn phần cho phong cách liquid glass, dark mode qua biến CSS |
| Song ngữ | **react-i18next**, file dịch `vi.json` / `en.json`; font **Be Vietnam Pro** (subset latin + vietnamese, line-height ≥ 1.5) | Dấu tiếng Việt hiển thị chuẩn |
| Email | Dịch vụ SMTP (Resend, Brevo, SES) | Xác minh email, quên mật khẩu |
| Triển khai | **Docker Compose trên 1 VPS** (2 vCPU / 4GB RAM là đủ), Nginx + HTTPS (Let's Encrypt) | Rẻ, dễ vận hành |
| Giám sát | Log ra file + Sentry (bản miễn phí) cho lỗi frontend/backend | Đủ cho 100 người |

## 2. Sơ đồ tổng quan

```
Trình duyệt (React)
   │  HTTPS (REST)            │  WSS (STOMP)
   ▼                          ▼
Nginx ──► Spring Boot app (API + WebSocket + Auth)
                │                    │
                ▼                    ▼
            MongoDB          Object storage (R2/MinIO)
                                ▲
Trình duyệt ── upload/download trực tiếp qua presigned URL
```

## 3. Mô hình dữ liệu MongoDB (phác thảo)

| Collection | Trường chính | Index |
|---|---|---|
| `users` | email, passwordHash, googleId, displayName, displayNameNormalized (bỏ dấu), avatarUrl, privacy, status | email (unique), googleId, displayNameNormalized |
| `friendships` | userA, userB, status (pending/accepted/blocked), requestedBy | {userA, userB} unique |
| `conversations` | type (direct/group), name, avatar, ownerId, settings, lastMessage (tóm tắt), lastSeq, updatedAt | updatedAt |
| `conversation_members` | conversationId, userId, role (owner/admin/member), joinedAt, lastReadSeq, mutedUntil | {userId, updatedAt}, {conversationId, userId} unique |
| `messages` | conversationId, **seq** (tăng dần theo hội thoại), senderId, type, content, attachments, replyTo, reactions, recalledAt, clientMsgId | {conversationId, seq} unique, {senderId, clientMsgId} unique |
| `push_subscriptions` | userId, endpoint, keys | userId |
| `reports` | reporterId, targetType, targetId, reason, status | status |

**Quyết định thiết kế quan trọng:**
- **`seq` theo từng hội thoại** (tăng bằng `findAndModify` trên `conversations.lastSeq`): đảm bảo thứ tự, phân trang ("lấy 30 tin có seq < X"), và tính số chưa đọc = `lastSeq - lastReadSeq`.
- **`clientMsgId`** do client sinh (UUID): chống tạo tin trùng khi gửi lại (story M3-12).
- Chat 1-1 cũng là một `conversation` loại `direct` → dùng chung code với nhóm.
- `lastMessage` nhúng vào `conversations` để load danh sách hội thoại bằng 1 truy vấn.
- Tìm kiếm không dấu: lưu thêm trường đã bỏ dấu, tìm bằng regex tiền tố (đủ nhanh ở 100 người).

## 4. Luồng realtime

1. Client kết nối WSS kèm JWT, subscribe kênh riêng `/user/queue/events`.
2. Gửi tin: client → `/app/messages.send` {conversationId, clientMsgId, content} → server lưu, cấp `seq` → trả ACK cho người gửi → đẩy sự kiện `message.new` tới mọi thành viên (tất cả thiết bị).
3. Người dùng không online trên thiết bị nào → gửi web push (trừ khi đang tắt thông báo).
4. Mất kết nối → client tự kết nối lại, gọi REST "lấy tin có seq > seq cuối cùng đã có" cho các hội thoại để bù tin bị lỡ.
5. Các sự kiện khác cùng kênh: `message.recalled`, `reaction.updated`, `read.updated`, `typing`, `presence`, `member.changed`.

Ghi chú mở rộng: khi cần chạy nhiều instance (Phase 2+), thay broker in-memory bằng relay tới RabbitMQ/Redis mà không đổi API phía client.

## 5. Việc cần làm trong tuần S0 (28/09 – 04/10)

- [ ] Kam xác nhận framework (Spring Boot?) và cấu trúc hiện tại của server.
- [ ] Chốt API contract (REST + sự kiện WebSocket) ở dạng OpenAPI / tài liệu ngắn.
- [ ] Dựng repo (monorepo `backend/` + `frontend/` hoặc 2 repo), CI build + test.
- [ ] Prototype: 2 trình duyệt nhắn tin với nhau qua STOMP (chứng minh rủi ro lớn nhất sớm).
- [ ] Wireframe 5 màn hình: đăng nhập, danh sách hội thoại + khung chat, thông tin nhóm, hồ sơ, cài đặt.
