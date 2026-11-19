# Web Chat – API Contract MVP (v0.2)

_Người lập: Claude (Senior PM) · Ngày: 2026-09-26 · Phạm vi: MVP gọn (120 SP) · Backend: Java (Spring Boot, giả định) + MongoDB · Frontend: React + shadcn/ui_

Tài liệu này là "hợp đồng" giữa frontend và backend để hai bên làm song song. Mọi thay đổi phải được cập nhật ở đây trước khi code.

**Liên kết:** story ở `02-user-stories-mvp.md`, kiến trúc ở `04-kien-truc-ky-thuat.md`.

---

## 0. Quy ước chung

| Hạng mục | Quy ước |
|---|---|
| Base URL | `https://<domain>/api/v1` |
| Định dạng | JSON, tên trường `camelCase`, UTF-8 |
| ID | Chuỗi (ObjectId MongoDB dạng hex 24 ký tự) |
| Thời gian | ISO-8601 UTC, ví dụ `2026-10-05T09:41:00Z`; client tự đổi múi giờ |
| Xác thực | `Authorization: Bearer <accessToken>` (JWT, sống 15 phút). Refresh token sống 30 ngày, xoay vòng mỗi lần refresh; nằm trong cookie `httpOnly; Secure; SameSite=Strict` (Kam chốt 26/09, mục 2.1) |
| Phân trang danh sách | Cursor: `?limit=20&cursor=<opaque>` → trả `{ items, nextCursor }` (`nextCursor = null` là hết) |
| Phân trang tin nhắn | Theo `seq`: `?beforeSeq=` (cuộn lên) hoặc `?afterSeq=` (bù tin khi kết nối lại) |
| Song ngữ | Server **không** trả câu thông báo đã dịch; chỉ trả **mã lỗi** và **dữ liệu có cấu trúc**, client dịch bằng `react-i18next`. Email (xác minh, quên mật khẩu) gửi theo `user.settings.language` |
| Idempotency | Gửi tin nhắn bắt buộc có `clientMsgId` (UUID v4 do client sinh); server trả lại tin cũ nếu trùng |
| Giới hạn tốc độ | Vượt giới hạn → HTTP `429`, header `Retry-After` (giây) |

### 0.1 Định dạng lỗi

```json
{
  "error": {
    "code": "RECALL_WINDOW_EXPIRED",
    "message": "Recall is only allowed within 24 hours",
    "details": { "messageId": "66f5..." }
  }
}
```

`message` chỉ để debug (tiếng Anh); UI hiển thị theo `code`. Danh sách mã lỗi ở mục 5.

### 0.2 HTTP status dùng thống nhất

`200` OK · `201` Tạo mới · `204` Không có nội dung · `400` Sai dữ liệu · `401` Chưa đăng nhập / token hết hạn · `403` Không có quyền · `404` Không tìm thấy · `409` Xung đột (trùng email, đã là bạn…) · `413` File quá lớn · `422` Vi phạm quy tắc nghiệp vụ · `423` Tài khoản tạm khoá · `429` Quá giới hạn tốc độ.

---

## 1. Mô hình dữ liệu (DTO)

```ts
// Dạng TypeScript để frontend dùng trực tiếp; backend map sang Java record tương ứng.

type UserSummary = {
  id: string;
  displayName: string;
  avatarUrl: string | null;
};

type Me = UserSummary & {
  email: string;
  emailVerified: boolean;
  bio: string | null;
  authProviders: ("password" | "google")[];
  settings: UserSettings;
  createdAt: string;
};

type UserSettings = {
  language: "vi" | "en";
  theme: "light" | "dark" | "system";
  whoCanMessage: "everyone" | "friends";   // M16-03
  soundEnabled: boolean;
};

type Conversation = {
  id: string;
  type: "direct" | "group";
  name: string | null;          // group: tên nhóm; null = client tự ghép tên 3 thành viên đầu
  avatarUrl: string | null;
  peer: UserSummary | null;     // chỉ có ở direct: người còn lại
  ownerId: string | null;       // chỉ có ở group
  memberCount: number;
  lastMessage: MessagePreview | null;
  lastSeq: number;
  me: {
    role: "owner" | "member" | null;   // null ở direct
    lastReadSeq: number;
    unreadCount: number;               // = lastSeq - lastReadSeq (không tính tin của chính mình)
    mutedUntil: string | null;         // "9999-12-31T00:00:00Z" = tắt đến khi bật lại
  };
  status: "active" | "dissolved";
  updatedAt: string;
};

type Member = {
  user: UserSummary;
  role: "owner" | "member";
  joinedAt: string;
  lastReadSeq: number;
  lastDeliveredSeq: number;
};

type Message = {
  id: string;
  conversationId: string;
  seq: number;                  // tăng dần trong từng hội thoại, bắt đầu từ 1
  clientMsgId: string | null;
  sender: UserSummary | null;   // null với tin hệ thống
  type: "text" | "image" | "video" | "file" | "system";
  content: string | null;       // text: nội dung (tối đa 5.000 ký tự)
  attachments: Attachment[];    // image/video/file: tối đa 20 ảnh hoặc 1 file/video
  system: SystemEvent | null;   // chỉ có ở type = "system"
  replyTo: { id: string; seq: number; sender: UserSummary; preview: string; type: Message["type"] } | null;
  reactions: Record<ReactionType, number>;   // chỉ chứa loại có số > 0
  myReaction: ReactionType | null;
  recalled: boolean;            // true: content, attachments = null/[]; UI hiện "Tin nhắn đã được thu hồi"
  createdAt: string;
};

type MessagePreview = Pick<Message, "id" | "seq" | "sender" | "type" | "recalled" | "createdAt"> & {
  text: string | null;          // 100 ký tự đầu; client tự hiển thị "[Ảnh]", "[File]"… theo type
};

type ReactionType = "like" | "love" | "haha" | "wow" | "sad" | "angry";

type Attachment = {
  fileId: string;
  name: string;
  mimeType: string;
  size: number;                 // byte
  url: string;                  // presigned GET, hết hạn sau 1 giờ
  thumbUrl: string | null;      // ảnh/video
  width: number | null;
  height: number | null;
  durationSec: number | null;   // video
};

// Tin hệ thống lưu dạng có cấu trúc để client dịch được sang cả hai ngôn ngữ
type SystemEvent = {
  event: "group.created" | "member.added" | "member.removed" | "member.left"
       | "group.renamed" | "group.avatarChanged" | "owner.transferred" | "group.dissolved";
  actor: UserSummary;
  targets: UserSummary[];
  data: Record<string, string>;   // ví dụ { "newName": "Nhóm Dự án" }
};

type FriendRequest = {
  id: string;
  from: UserSummary;
  to: UserSummary;
  status: "pending" | "accepted" | "declined" | "cancelled";
  createdAt: string;
};

type RelationStatus = "none" | "friend" | "outgoing_request" | "incoming_request" | "self";
```

---

## 2. REST API

Ký hiệu cột **Story**: mã story trong `02-user-stories-mvp.md`. 🔓 = không cần đăng nhập.

### 2.1 Xác thực (M1) – khớp với `AuthController` của Kam

> Đã đối chiếu với `AuthController.java` (Spring Boot) ngày 26/09. Đăng ký dùng **OTP 6 số gửi qua email** (không dùng link). Tài khoản chỉ được tạo sau khi xác thực OTP thành công.
> Base path hiện tại trên server là `/api/auth`; đề xuất đổi thành `/api/v1/auth` để thống nhất với các module khác (mục 0).

| Method | Path | Trạng thái trên server | Mô tả | Story |
|---|---|---|---|---|
| POST 🔓 | `/auth/register` | ✅ Có | Gửi thông tin đăng ký, server gửi OTP tới email → `202` | M1-01 |
| POST 🔓 | `/auth/register/resend-otp` | ✅ Có | Gửi lại OTP → `202` | M1-02 |
| POST 🔓 | `/auth/register/verify` | ✅ Có | Xác thực OTP, tạo tài khoản và đăng nhập luôn → `201` | M1-02 |
| POST 🔓 | `/auth/login` | ✅ Có | Đăng nhập email + mật khẩu | M1-03 |
| POST 🔓 | `/auth/google` | ✅ Có | Đăng nhập/đăng ký bằng Google ID token | M1-08 |
| POST 🔓 | `/auth/refresh` | 🔧 Sửa: đọc refresh token từ cookie | Lấy access token mới | M1-05 |
| POST | `/auth/logout` | 🔧 Sửa: đọc refresh token từ cookie | Đăng xuất thiết bị hiện tại, xoá cookie → `204` | M1-06 |
| POST 🔓 | `/auth/password/forgot` | ❌ Chưa có | Gửi OTP đặt lại mật khẩu tới email → `202` | M1-04 |
| POST 🔓 | `/auth/password/reset` | ❌ Chưa có | Đặt mật khẩu mới bằng OTP → `204` | M1-04 |

**Chi tiết (đề xuất nội dung DTO; Kam đối chiếu với DTO thực tế):**

```http
POST /auth/register                                   RegisterRequest
{ "email": "kam@email.com", "password": "********", "displayName": "Kam", "language": "vi" }
→ 202 { "email": "kam@email.com", "otpExpiresAt": "...", "resendAvailableAt": "..." }   RegisterResponse
✗ 409 EMAIL_TAKEN · 400 VALIDATION_FAILED (password < 8, displayName ngoài 2–50 ký tự)

POST /auth/register/resend-otp                        ResendOtpRequest
{ "email": "kam@email.com" }
→ 202 RegisterResponse
✗ 429 RATE_LIMITED { "details": { "retryAfterSec": 45 } }   (tối đa 5 lần/giờ, cách nhau ≥ 60 giây)

POST /auth/register/verify                            VerifyOtpRequest
{ "email": "kam@email.com", "otp": "482913" }
→ 201 AuthResponse
✗ 400 OTP_INVALID { "details": { "attemptsLeft": 3 } } · 410 OTP_EXPIRED (sau 5 phút)
✗ 423 OTP_LOCKED (nhập sai 5 lần → khoá 15 phút)

POST /auth/login                                      LoginRequest
{ "email": "kam@email.com", "password": "********" }
→ 200 AuthResponse
✗ 401 AUTH_INVALID_CREDENTIALS
✗ 423 AUTH_LOCKED { "details": { "retryAfterSec": 840 } }  (sai 5 lần → khoá 15 phút)

POST /auth/google                                     GoogleLoginRequest
{ "idToken": "<Google Identity Services credential>" }
→ 200 AuthResponse (kèm "isNewUser": true khi vừa tạo tài khoản)
✗ 401 GOOGLE_TOKEN_INVALID
✗ 409 ACCOUNT_LINK_REQUIRED  (email Google trùng tài khoản mật khẩu: yêu cầu đăng nhập mật khẩu trước rồi liên kết)

POST /auth/refresh                                    (không có body; cookie refresh_token)
→ 200 AuthResponse + Set-Cookie refresh_token mới (token cũ bị thu hồi – xoay vòng)
✗ 401 TOKEN_EXPIRED → client chuyển về màn đăng nhập
✗ 401 TOKEN_REUSED  (token đã bị thu hồi mà vẫn được dùng lại → thu hồi toàn bộ phiên của user)

POST /auth/logout                                     (không có body; cookie refresh_token)
→ 204 + Set-Cookie refresh_token rỗng, Max-Age=0

POST /auth/password/forgot                            (đề xuất thêm)
{ "email": "kam@email.com" }
→ 202  (luôn 202 kể cả email không tồn tại, tránh dò email)

POST /auth/password/reset                             (đề xuất thêm)
{ "email": "kam@email.com", "otp": "482913", "newPassword": "********" }
→ 204  (thu hồi mọi refresh token của user)
```

`AuthResponse` đề xuất:

```json
{
  "accessToken": "eyJ...",
  "accessTokenExpiresIn": 900,
  "user": { "...Me" }
}
```

Mọi endpoint trả `AuthResponse` (`/register/verify`, `/login`, `/google`, `/refresh`) đồng thời đặt cookie:

```
Set-Cookie: refresh_token=<token>; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=2592000
```

Frontend giữ access token **trong bộ nhớ** (không lưu localStorage); khi tải lại trang thì gọi `/auth/refresh` để lấy access token mới. Gợi ý Spring Boot:

```java
ResponseCookie cookie = ResponseCookie.from("refresh_token", refreshToken)
        .httpOnly(true).secure(true).sameSite("Strict")
        .path("/api/v1/auth").maxAge(Duration.ofDays(30)).build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

@PostMapping("/refresh")
public AuthResponse refresh(@CookieValue("refresh_token") String refreshToken,
                            HttpServletResponse response) { ... }
```

Môi trường dev chạy frontend (Vite) và backend khác cổng: dùng proxy của Vite (`server.proxy['/api']`) để cookie vẫn là same-site, không cần mở CORS có credentials.

**Quyết định 26/09 (Kam):** refresh token chuyển sang cookie `httpOnly` để JavaScript không đọc được, tránh bị lấy cắp khi có lỗi XSS.

### 2.2 Người dùng & hồ sơ (M2, M16)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/users/me` | Thông tin của tôi | M2-01 |
| PATCH | `/users/me` | Sửa `displayName`, `bio`, `avatarFileId` | M2-01 |
| PATCH | `/users/me/settings` | Sửa `language`, `theme`, `whoCanMessage`, `soundEnabled` | M16-01…04 |
| GET | `/users/search?email=` | Tìm người bằng email **khớp chính xác** | M2-02 |
| GET | `/users/{userId}` | Hồ sơ công khai + quan hệ với tôi | M2-02 |

```http
GET /users/search?email=phuongha@email.com
→ 200 { "user": UserSummary, "relation": RelationStatus }
✗ 404 NOT_FOUND  (không có, hoặc người đó ẩn khỏi tìm kiếm)
```

### 2.3 Bạn bè (M2)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/friends?cursor=&limit=` | Danh sách bạn bè (sắp theo tên) | M2-05 |
| DELETE | `/friends/{userId}` | Huỷ kết bạn | M2-05 |
| GET | `/friend-requests?direction=incoming\|outgoing` | Lời mời đã nhận / đã gửi (đang chờ) | M2-03, M2-04 |
| POST | `/friend-requests` | Gửi lời mời `{ "toUserId": "..." }` | M2-03 |
| POST | `/friend-requests/{id}/accept` | Đồng ý | M2-04 |
| POST | `/friend-requests/{id}/decline` | Từ chối | M2-04 |
| DELETE | `/friend-requests/{id}` | Thu hồi lời mời mình đã gửi | M2-03 |

Quy tắc: gửi cho người đã gửi lời mời cho mình → tự chấp nhận luôn. ✗ `409 ALREADY_FRIENDS` · `409 REQUEST_EXISTS` · `429 FRIEND_REQUEST_LIMIT` (30/ngày).

### 2.4 Hội thoại & nhóm (M3, M4)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/conversations?cursor=&limit=20` | Danh sách hội thoại, sắp theo `updatedAt` giảm dần | M3-01 |
| GET | `/conversations?updatedAfter=<ISO>` | Lấy các hội thoại thay đổi sau thời điểm (dùng khi kết nối lại) | M3-12 |
| GET | `/conversations/{id}` | Chi tiết 1 hội thoại | M3-01 |
| POST | `/conversations/direct` | Mở (hoặc tạo) chat 1-1 `{ "userId": "..." }` | M3-02 |
| POST | `/conversations/group` | Tạo nhóm `{ "name"?, "avatarFileId"?, "memberIds": [...] }` | M4-01 |
| PATCH | `/conversations/{id}` | Đổi `name`, `avatarFileId` (trưởng nhóm) | M4-01 |
| DELETE | `/conversations/{id}` | Giải tán nhóm (trưởng nhóm) | M4-06 |
| GET | `/conversations/{id}/members` | Danh sách thành viên | M4-11 |
| POST | `/conversations/{id}/members` | Thêm thành viên `{ "userIds": [...] }` (trưởng nhóm) | M4-03 |
| DELETE | `/conversations/{id}/members/{userId}` | Xoá thành viên (trưởng nhóm) | M4-03 |
| POST | `/conversations/{id}/leave` | Rời nhóm `{ "newOwnerId"? }` (bắt buộc nếu là trưởng nhóm) | M4-05 |
| PUT | `/conversations/{id}/mute` | Tắt thông báo `{ "until": ISO \| "forever" \| null }` | M6-03 |
| POST | `/conversations/{id}/read` | Đánh dấu đã đọc tới `{ "seq": 128 }` (REST dự phòng cho WS) | M3-03 |

Quy tắc chính:
- `POST /conversations/direct` idempotent: đã có thì trả về hội thoại cũ (`200`), chưa có thì tạo (`201`). ✗ `403 MESSAGING_RESTRICTED` nếu người kia đặt "chỉ bạn bè".
- Tạo nhóm: `memberIds` là bạn bè của người tạo, 2 ≤ số người ≤ 99 (tổng ≤ 100). ✗ `422 GROUP_TOO_SMALL` · `422 GROUP_FULL` · `422 NOT_FRIENDS`.
- Trưởng nhóm gọi `leave` không kèm `newOwnerId` → ✗ `422 OWNER_MUST_TRANSFER`.
- Thành viên mới chỉ thấy tin có `seq` > `lastSeq` tại thời điểm vào nhóm (server lưu `joinedSeq`).
- Mọi thay đổi thành viên/tên nhóm sinh **tin hệ thống** (`type: "system"`) và sự kiện WS.
- Nhóm đã giải tán: `status = "dissolved"`, chỉ đọc; mọi thao tác ghi ✗ `422 CONVERSATION_DISSOLVED`.

### 2.5 Tin nhắn (M3, M5)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/conversations/{id}/messages?beforeSeq=&limit=30` | Tải lịch sử (không có `beforeSeq` = 30 tin mới nhất) | M3-11 |
| GET | `/conversations/{id}/messages?afterSeq=&limit=100` | Bù tin bị lỡ sau khi mất kết nối | M3-12 |
| POST | `/conversations/{id}/messages` | Gửi tin (REST dự phòng; đường chính là WS, mục 3) | M3-02 |
| POST | `/messages/{id}/recall` | Thu hồi tin của chính mình trong 24 giờ | M3-07 |
| PUT | `/messages/{id}/reaction` | Thả/đổi reaction `{ "type": "love" }` | M3-06 |
| DELETE | `/messages/{id}/reaction` | Gỡ reaction của mình | M3-06 |

```http
GET /conversations/{id}/messages?beforeSeq=120&limit=30
→ 200 { "items": Message[], "hasMore": true }     // items sắp theo seq tăng dần

POST /conversations/{id}/messages
{
  "clientMsgId": "7f1c0d9e-...",
  "type": "text",
  "content": "Ổn rồi, mình góp ý thêm phần thanh nhập tin nhắn.",
  "attachmentFileIds": [],
  "replyToId": "66f5..."
}
→ 201 Message   (hoặc 200 Message cũ nếu clientMsgId đã tồn tại)
✗ 400 VALIDATION_FAILED (rỗng, > 5.000 ký tự) · 403 NOT_MEMBER · 403 MESSAGING_RESTRICTED
✗ 429 RATE_LIMITED (> 20 tin/10 giây → chặn 30 giây)

POST /messages/{id}/recall
→ 200 Message (recalled = true)
✗ 403 NOT_SENDER · 422 RECALL_WINDOW_EXPIRED
```

Quy tắc: thu hồi xoá nội dung và file khỏi lưu trữ; `replyTo.preview` của các tin trích dẫn nó cũng hiển thị "đã thu hồi". Mỗi người 1 reaction/tin.

### 2.6 File & media (M5)

Upload đi **thẳng lên object storage** qua presigned URL, không qua server Java.

| Method | Path | Mô tả | Story |
|---|---|---|---|
| POST | `/uploads` | Xin URL upload | M5-01, M5-02 |
| POST | `/uploads/{fileId}/complete` | Báo upload xong, server kiểm tra và tạo thumbnail | M5-01, M5-02 |

```http
POST /uploads
{ "fileName": "user-stories-mvp.pdf", "mimeType": "application/pdf", "size": 1258291,
  "purpose": "attachment" }          // "attachment" | "avatar" | "group_avatar"
→ 201 { "fileId": "...", "uploadUrl": "https://...", "method": "PUT",
        "headers": { "Content-Type": "application/pdf" }, "expiresAt": "..." }
✗ 413 FILE_TOO_LARGE (> 100MB; avatar > 5MB) · 422 FILE_TYPE_BLOCKED (.exe, .bat, .msi, .apk, .cmd, .scr…)

POST /uploads/{fileId}/complete
→ 200 Attachment
✗ 422 UPLOAD_NOT_FOUND (client chưa PUT xong) · 422 FILE_MISMATCH (size/mime thực tế khác khai báo)
```

Luồng gửi ảnh: `POST /uploads` → client `PUT` file lên `uploadUrl` (hiện tiến trình) → `POST /uploads/{id}/complete` → gửi tin kèm `attachmentFileIds`. File không được gắn vào tin nào sau 24 giờ sẽ bị xoá.

### 2.7 Tìm kiếm (M7)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/search?q=nguyen&limit=10` | Tìm bạn bè và nhóm theo tên, không phân biệt dấu | M7-01 |

```http
→ 200 { "friends": UserSummary[], "groups": Conversation[] }
```

### 2.8 Web push (M6)

| Method | Path | Mô tả | Story |
|---|---|---|---|
| GET | `/push/vapid-public-key` | Khoá công khai VAPID | M6-01 |
| POST | `/push/subscriptions` | Đăng ký `{ endpoint, keys: { p256dh, auth } }` | M6-01 |
| DELETE | `/push/subscriptions` | Huỷ `{ endpoint }` (khi đăng xuất) | M6-01 |

Nội dung push: `{ "type": "message.new", "conversationId", "title": "<tên người gửi hoặc nhóm>", "body": "<50 ký tự đầu hoặc mã loại tin>", "seq" }`. Service worker dịch "[Ảnh]", "[File]" theo ngôn ngữ đã lưu. Không gửi push nếu người nhận đang có kết nối WS với tab đang focus vào đúng hội thoại, hoặc hội thoại đang tắt thông báo (ngoại lệ "bị @mention vẫn nhận thông báo" sẽ thêm khi làm @mention sau MVP).

---

## 3. Realtime: WebSocket + STOMP

| Hạng mục | Giá trị |
|---|---|
| Endpoint | `wss://<domain>/ws` (STOMP 1.2 trên WebSocket thuần, không dùng SockJS) |
| Xác thực | Header `Authorization: Bearer <accessToken>` trong frame `CONNECT`. Token hết hạn → server đóng kết nối với lỗi `TOKEN_EXPIRED`, client refresh rồi kết nối lại |
| Heartbeat | `10000,10000` (10 giây) |
| Kênh nhận | Client **chỉ** subscribe `/user/queue/events` (mọi sự kiện) và `/user/queue/acks` (xác nhận gửi tin) |

### 3.1 Client → Server (`SEND`)

| Destination | Payload | Mô tả | Story |
|---|---|---|---|
| `/app/messages.send` | `{ clientMsgId, conversationId, type, content, attachmentFileIds, replyToId }` | Gửi tin nhắn | M3-02 |
| `/app/messages.delivered` | `{ conversationId, seq }` | Báo đã nhận tới `seq` (gửi khi nhận `message.new`) | M3-03 |
| `/app/messages.read` | `{ conversationId, seq }` | Báo đã xem tới `seq` (chỉ khi hội thoại đang mở và tab focus) | M3-03 |
| `/app/typing` | `{ conversationId, isTyping }` | Đang soạn tin (gửi tối đa 1 lần/3 giây; tự hết sau 5 giây) | M3-04 |
| `/app/focus` | `{ conversationId \| null }` | Hội thoại đang mở ở tab này (để quyết định có gửi push hay không) | M6-01 |

### 3.2 Server → Client: xác nhận gửi tin (`/user/queue/acks`)

```json
{ "clientMsgId": "7f1c0d9e-...", "ok": true, "message": { "...Message" } }
{ "clientMsgId": "7f1c0d9e-...", "ok": false, "error": { "code": "RATE_LIMITED", "details": { "retryAfterSec": 30 } } }
```

Client đổi trạng thái tin: **Đang gửi** (chưa có ack) → **Đã gửi** (ack ok) → **Đã nhận** (`delivery.updated`) → **Đã xem** (`read.updated`). Quá 10 giây không có ack → hiện "Gửi không thành công · Gửi lại" (gửi lại với **cùng** `clientMsgId`).

### 3.3 Server → Client: sự kiện (`/user/queue/events`)

Mọi sự kiện có chung vỏ:

```json
{ "type": "message.new", "ts": "2026-10-05T09:41:00Z", "data": { } }
```

| `type` | `data` | Gửi cho |
|---|---|---|
| `message.new` | `Message` | Mọi thành viên (mọi thiết bị, kể cả thiết bị khác của người gửi) |
| `message.recalled` | `{ conversationId, messageId, seq }` | Mọi thành viên |
| `reaction.updated` | `{ conversationId, messageId, reactions, userId, type \| null }` | Mọi thành viên |
| `delivery.updated` | `{ conversationId, userId, seq }` | Mọi thành viên |
| `read.updated` | `{ conversationId, userId, seq }` | Mọi thành viên (thiết bị khác của chính mình dùng để xoá badge chưa đọc) |
| `typing` | `{ conversationId, user: UserSummary, isTyping }` | Thành viên khác |
| `conversation.created` | `Conversation` | Thành viên được thêm vào hội thoại mới |
| `conversation.updated` | `Conversation` | Mọi thành viên (đổi tên, ảnh, thành viên, tắt thông báo…) |
| `conversation.removed` | `{ conversationId, reason: "left" \| "removed" }` | Người rời/bị xoá khỏi nhóm |
| `friend.requested` | `FriendRequest` | Người nhận lời mời |
| `friend.accepted` | `{ user: UserSummary }` | Người gửi lời mời |
| `settings.updated` | `UserSettings` | Các thiết bị khác của chính mình (đồng bộ theme, ngôn ngữ) |

### 3.4 Kết nối lại và bù tin

1. Mất kết nối → STOMP.js tự kết nối lại (backoff 1s, 2s, 5s, tối đa 30s).
2. Kết nối lại thành công → `GET /conversations?updatedAfter=<thời điểm sự kiện cuối cùng đã nhận>`.
3. Với mỗi hội thoại đang có trong bộ nhớ mà `lastSeq` mới > `seq` cuối client có → `GET /messages?afterSeq=`.
4. Tin nhắn trong hàng chờ gửi (offline) được gửi lại theo thứ tự, cùng `clientMsgId`.

### 3.5 Trình tự gửi một tin nhắn

```
Client A                    Server                          Client B (2 thiết bị)
   │ SEND /app/messages.send   │                                  │
   │──────────────────────────►│ lưu Mongo, cấp seq=129            │
   │◄─ /user/queue/acks ───────│                                  │
   │   (Đã gửi)                │── message.new ───────────────────►│ (cả 2 thiết bị)
   │                           │◄─ /app/messages.delivered seq=129 │
   │◄─ delivery.updated ───────│                                  │
   │   (Đã nhận)               │◄─ /app/messages.read seq=129 ─────│ (khi B mở hội thoại)
   │◄─ read.updated ───────────│── read.updated ──────────────────►│ (thiết bị kia của B xoá badge)
   │   (Đã xem)                │                                  │
   │                           │ B không online → gửi web push     │
```

---

## 4. Giới hạn & ràng buộc (tổng hợp)

| Ràng buộc | Giá trị | Mã lỗi |
|---|---|---|
| Độ dài tin nhắn | 5.000 ký tự | `VALIDATION_FAILED` |
| Tốc độ gửi tin | 20 tin / 10 giây / người → chặn 30 giây | `RATE_LIMITED` |
| Lời mời kết bạn | 30 / ngày | `FRIEND_REQUEST_LIMIT` |
| Thành viên nhóm | 3 – 100 | `GROUP_TOO_SMALL`, `GROUP_FULL` |
| File | 100MB; avatar 5MB; 20 ảnh/tin | `FILE_TOO_LARGE` |
| Thu hồi | trong 24 giờ | `RECALL_WINDOW_EXPIRED` |
| Đăng nhập sai | 5 lần → khoá 15 phút | `AUTH_LOCKED` |
| Gửi lại OTP | 5 lần / giờ, cách nhau ≥ 60 giây | `RATE_LIMITED` |
| OTP | 6 số, hết hạn sau 5 phút, sai 5 lần → khoá 15 phút | `OTP_INVALID`, `OTP_EXPIRED`, `OTP_LOCKED` |
| Tên hiển thị | 2 – 50 ký tự | `VALIDATION_FAILED` |

## 5. Danh sách mã lỗi

| Mã | HTTP | Ý nghĩa (để viết bản dịch vi/en) |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Dữ liệu không hợp lệ; `details.fields` liệt kê trường lỗi |
| `TOKEN_INVALID` | 400 | Token không hợp lệ |
| `OTP_INVALID` | 400 | Mã OTP sai; `details.attemptsLeft` |
| `OTP_EXPIRED` | 410 | Mã OTP đã hết hạn |
| `OTP_LOCKED` | 423 | Nhập sai OTP quá nhiều lần |
| `TOKEN_REUSED` | 401 | Refresh token bị dùng lại, mọi phiên đã bị đăng xuất |
| `TOKEN_EXPIRED` | 401/410 | Phiên hoặc link đã hết hạn |
| `UNAUTHORIZED` | 401 | Chưa đăng nhập |
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai email hoặc mật khẩu |
| `GOOGLE_TOKEN_INVALID` | 401 | Đăng nhập Google thất bại |
| `FORBIDDEN` | 403 | Không có quyền (vd. không phải trưởng nhóm) |
| `NOT_MEMBER` | 403 | Không thuộc hội thoại |
| `NOT_SENDER` | 403 | Chỉ người gửi mới thu hồi được |
| `MESSAGING_RESTRICTED` | 403 | Người nhận chỉ cho bạn bè nhắn tin |
| `NOT_FOUND` | 404 | Không tìm thấy |
| `EMAIL_TAKEN` | 409 | Email đã được đăng ký |
| `ACCOUNT_LINK_REQUIRED` | 409 | Cần xác nhận mật khẩu để liên kết Google |
| `ALREADY_FRIENDS` | 409 | Đã là bạn bè |
| `REQUEST_EXISTS` | 409 | Đã có lời mời đang chờ |
| `FILE_TOO_LARGE` | 413 | File vượt dung lượng cho phép |
| `FILE_TYPE_BLOCKED` | 422 | Loại file bị chặn |
| `UPLOAD_NOT_FOUND` / `FILE_MISMATCH` | 422 | Upload chưa hoàn tất hoặc file không khớp |
| `GROUP_TOO_SMALL` / `GROUP_FULL` | 422 | Số thành viên không hợp lệ |
| `NOT_FRIENDS` | 422 | Chỉ thêm được bạn bè vào nhóm |
| `OWNER_MUST_TRANSFER` | 422 | Trưởng nhóm phải chuyển quyền trước khi rời |
| `CONVERSATION_DISSOLVED` | 422 | Nhóm đã giải tán |
| `RECALL_WINDOW_EXPIRED` | 422 | Quá 24 giờ, không thu hồi được |
| `AUTH_LOCKED` | 423 | Tạm khoá đăng nhập; `details.retryAfterSec` |
| `FRIEND_REQUEST_LIMIT` | 429 | Quá 30 lời mời trong ngày |
| `RATE_LIMITED` | 429 | Thao tác quá nhanh; `details.retryAfterSec` |
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống |

## 6. Ngoài phạm vi MVP gọn (để dành chỗ, chưa làm)

Chặn người dùng, báo cáo vi phạm và trang admin, phó nhóm, @mention, trạng thái online/truy cập lần cuối, xoá tin phía tôi, chuyển tiếp, ghim, link preview, kho media, link mời nhóm, duyệt thành viên. Khi làm sẽ thêm endpoint/sự kiện mới, **không đổi** cấu trúc hiện có (ví dụ thêm `presence.updated`, `message.pinned`).

## 7. Việc cần Kam xác nhận

1. ~~Endpoint xác thực~~ → đã đối chiếu với `AuthController` (26/09). Việc cần sửa trên server: đổi base path sang `/api/v1/auth`, thêm quên mật khẩu, chuyển refresh token sang cookie httpOnly (đã chốt).
2. ~~Spring Boot?~~ → Có (Kam xác nhận 26/09).
3. Cách phát hành tài liệu cho đội: đề xuất dùng **springdoc-openapi** sinh Swagger UI từ code Java, còn tài liệu này là bản gốc cho phần WebSocket và quy tắc nghiệp vụ.
