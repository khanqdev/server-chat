# Web Chat – Phân tích nghiệp vụ & đề xuất module (v0.1)

_Người lập: Claude (vai trò Senior PM) · Ngày: 2026-09-26 · Trạng thái: **Đã chốt phạm vi MVP theo phương án 1 (Kam, 2026-09-26)**_

---

## 1. Bối cảnh & giả định

| Hạng mục | Giả định mặc định (cần Kam xác nhận) |
|---|---|
| Nền tảng | Web (desktop browser) trước, responsive cho mobile web; app native để sau |
| Đối tượng | Người dùng cá nhân + nhóm nhỏ/team (giống Zalo/Messenger), chưa nhắm B2B doanh nghiệp lớn |
| Thị trường | Việt Nam trước (đăng nhập bằng SĐT/email, tiếng Việt) |
| Quy mô MVP | ~1.000–10.000 người dùng, nhóm tối đa ~100 thành viên |
| Mô hình kinh doanh | Chưa xác định (miễn phí trước, freemium/doanh nghiệp tính sau) |

## 2. Học hỏi từ sản phẩm hiện có

| Sản phẩm | Điểm mạnh nên học | Điểm cần tránh / khác biệt |
|---|---|---|
| **Zalo** | Kết bạn qua SĐT, danh bạ; nhóm lớn; gọi thoại/video ổn định; "Cloud của tôi" (tự nhắn cho mình); thu hồi tin nhắn; tin nhắn trả lời/trích dẫn; bình chọn, nhắc hẹn, ghi chú nhóm | Giao diện web nặng; giới hạn dung lượng file; quyền riêng tư người lạ nhắn tin |
| **Messenger** | Reaction, reply, typing indicator, trạng thái "đã xem"; tin nhắn chờ (người lạ); chủ đề/biệt danh cuộc trò chuyện; mã hoá đầu-cuối | Phụ thuộc tài khoản Facebook |
| **Telegram** | Đồng bộ đa thiết bị tức thì; nhóm siêu lớn, channel; bot; file lớn; tìm kiếm mạnh; sửa tin nhắn | Kiểm duyệt nội dung khó |
| **WhatsApp** | Mã hoá đầu-cuối mặc định; đơn giản; community | Đa thiết bị web phụ thuộc điện thoại (giai đoạn đầu) |
| **Slack / Teams** | Thread, mention, tìm kiếm, huddle/meeting, tích hợp | Quá "công việc" cho người dùng cá nhân |
| **Google Meet / Zoom** | Họp nhóm lớn, chia sẻ màn hình, phòng chờ, ghi hình | Tách rời khỏi chat |

**Kết luận định vị:** Một web chat "Zalo/Messenger-like" cho cá nhân & nhóm, trải nghiệm web mượt, gọi/họp tích hợp ngay trong cuộc trò chuyện.

## 3. Bản đồ module & độ ưu tiên

Ký hiệu: **P0** = bắt buộc cho MVP · **P1** = ngay sau MVP (phase 2) · **P2** = mở rộng sau

| # | Module | Ưu tiên | Ghi chú |
|---|---|---|---|
| M1 | Tài khoản & xác thực | P0 | Nền tảng cho mọi thứ |
| M2 | Hồ sơ & danh bạ (bạn bè) | P0 | Kết bạn, chặn |
| M3 | Nhắn tin 1-1 | P0 | Lõi sản phẩm |
| M4 | Nhóm chat | P0 | Lõi sản phẩm |
| M5 | Gửi file & media | P0 | Ảnh, file, sticker/emoji cơ bản |
| M6 | Thông báo & trạng thái online | P0 | Web push, badge chưa đọc |
| M7 | Tìm kiếm | P0 (cơ bản) / P1 (nâng cao) | Tìm người, nhóm; tìm nội dung tin nhắn ở P1 |
| M8 | Gọi thoại 1-1 | P1 | WebRTC, cần TURN server |
| M9 | Video call 1-1 | P1 | Cùng hạ tầng với M8 |
| M10 | Gọi nhóm / Họp nhóm (meeting) | P2 | Cần SFU (media server), chi phí cao |
| M11 | Quản trị & kiểm duyệt (Admin, báo cáo vi phạm) | P0 (tối thiểu) / P1 | Bắt buộc có cơ chế báo cáo, khoá tài khoản |
| M12 | Tiện ích nhóm (bình chọn, ghim, nhắc hẹn, ghi chú) | P1 | Tăng giữ chân người dùng |
| M13 | Bảo mật & quyền riêng tư (E2EE, thiết bị đăng nhập) | P1 (quản lý phiên) / P2 (E2EE) | E2EE làm phức tạp tìm kiếm & đa thiết bị |
| M14 | Channel / Community (phát tin 1-nhiều) | P2 | |
| M15 | Bot & tích hợp (API, webhook) | P2 | |
| M16 | Cài đặt & cá nhân hoá (theme, ngôn ngữ, tắt thông báo) | P0 (cơ bản) | |

## 4. Quy tắc nghiệp vụ chính theo module

### M1. Tài khoản & xác thực (P0)
- Đăng ký bằng **email hoặc SĐT**, xác minh bằng OTP (SĐT) / link (email). OTP hết hạn sau 5 phút, tối đa 5 lần gửi/giờ.
- Đăng nhập: mật khẩu hoặc OTP; P1 thêm đăng nhập Google, đăng nhập bằng QR (quét từ thiết bị đã đăng nhập, như Zalo Web).
- Mật khẩu tối thiểu 8 ký tự; khoá tạm 15 phút sau 5 lần sai.
- Một tài khoản được đăng nhập nhiều thiết bị/trình duyệt cùng lúc; tin nhắn đồng bộ trên tất cả.
- Xoá tài khoản: có thời gian chờ 30 ngày trước khi xoá vĩnh viễn.

### M2. Hồ sơ & danh bạ (P0)
- Hồ sơ: tên hiển thị (bắt buộc), ảnh đại diện, ảnh bìa, giới thiệu, ngày sinh (tuỳ chọn).
- Tìm người bằng SĐT/email/username chính xác (không cho duyệt danh sách người dùng để chống thu thập dữ liệu).
- Kết bạn: gửi lời mời → chấp nhận/từ chối/thu hồi. Giới hạn ~30 lời mời/ngày để chống spam.
- **Người lạ nhắn tin:** cho phép nhưng tin vào mục "Tin nhắn chờ" (như Messenger); người dùng cài đặt được "chỉ bạn bè mới được nhắn".
- Chặn: người bị chặn không nhắn, không gọi, không thấy trạng thái online; không thông báo cho người bị chặn.

### M3. Nhắn tin 1-1 (P0)
- Loại tin: văn bản (tối đa ~5.000 ký tự), emoji, ảnh, file, link (có preview), sticker (P1), tin nhắn thoại (P1).
- Trạng thái tin: **Đang gửi → Đã gửi → Đã nhận → Đã xem**. Người dùng có thể tắt "đã xem" (P1).
- Typing indicator ("đang soạn tin").
- **Trả lời (reply/quote)**, **reaction** (bộ emoji cố định ~6 loại), **chuyển tiếp**, **ghim** tin nhắn.
- **Thu hồi** (xoá với mọi người): trong vòng 24 giờ, hiển thị "Tin nhắn đã được thu hồi".
- **Xoá phía tôi**: bất kỳ lúc nào, chỉ ẩn với mình.
- **Sửa tin nhắn** (P1): trong 15 phút, hiển thị nhãn "đã chỉnh sửa".
- Lịch sử lưu trên server, tải phân trang (cuộn lên để xem cũ hơn). Gửi khi mất mạng: xếp hàng và gửi lại tự động.
- Thứ tự tin nhắn đảm bảo theo thời điểm server nhận; không trùng lặp khi gửi lại (idempotency key).
- "Cloud của tôi" / nhắn cho chính mình: P1.

### M4. Nhóm chat (P0)
- Tạo nhóm từ ≥ 2 người khác (tổng ≥ 3). Giới hạn MVP: **100 thành viên** (P1 nâng lên 500–1.000).
- Vai trò: **Trưởng nhóm (Owner)** → **Phó nhóm (Admin)** → **Thành viên**.
  - Owner: mọi quyền, chuyển quyền trưởng nhóm, giải tán nhóm.
  - Admin: thêm/xoá thành viên, duyệt thành viên, ghim tin, đổi tên/ảnh nhóm.
  - Thành viên: nhắn tin, thêm người (nếu nhóm cho phép), rời nhóm.
- Owner rời nhóm phải chuyển quyền trước; nếu tài khoản bị xoá → tự chuyển cho Admin lâu nhất, không có thì thành viên lâu nhất.
- Cài đặt nhóm: chỉ Admin được đổi thông tin; bật "duyệt thành viên mới"; link mời tham gia (có thể thu hồi/tạo mới).
- @mention cá nhân và @all (chỉ Admin dùng @all ở nhóm > 20 người).
- Thành viên mới **không** xem được lịch sử trước khi vào (mặc định, giống Zalo) – có thể cấu hình ở P1.
- Tin hệ thống: "A đã thêm B vào nhóm", "C đã rời nhóm"...
- "Đã xem" trong nhóm: hiện avatar những người đã xem tin cuối (tối đa vài avatar + số).

### M5. File & media (P0)
- Ảnh: nén + tạo thumbnail; xem ảnh dạng gallery. Video: tối đa ~100MB ở MVP.
- File bất kỳ: tối đa **100MB/file** (MVP), chặn file thực thi nguy hiểm (.exe, .bat...) hoặc cảnh báo.
- Quét virus/mã độc khi tải lên (P1 nếu hạn chế ngân sách, nhưng khuyến nghị sớm).
- Kho media của cuộc trò chuyện: tab Ảnh/Video, File, Link.
- Thời gian lưu trữ: không giới hạn ở MVP; cân nhắc chính sách dọn file > 1 năm để kiểm soát chi phí.

### M6. Thông báo & trạng thái (P0)
- Web push notification (khi tab đóng/không focus) + âm báo + số chưa đọc trên tab.
- Tắt thông báo theo cuộc trò chuyện: 1 giờ / 8 giờ / đến khi bật lại.
- Không gửi thông báo trùng khi người dùng đang mở đúng cuộc trò chuyện đó trên thiết bị khác.
- Trạng thái: **Online / Truy cập X phút trước**; người dùng có thể ẩn (P1).

### M7. Tìm kiếm (P0 cơ bản, P1 nâng cao)
- P0: tìm bạn bè, nhóm, cuộc trò chuyện theo tên (hỗ trợ không dấu tiếng Việt).
- P1: tìm nội dung tin nhắn trong 1 cuộc trò chuyện và toàn bộ; lọc theo người gửi, thời gian, loại file.

### M8–M9. Gọi thoại & video 1-1 (P1)
- Công nghệ: WebRTC P2P, STUN + **TURN** (bắt buộc cho ~15–20% kết nối không P2P được).
- Luồng: Gọi → Đổ chuông (tối đa 45–60 giây) → Nghe / Từ chối / Nhỡ. Chỉ bạn bè mới gọi được (cấu hình).
- Đang trong cuộc gọi khác → báo "máy bận" cho người gọi.
- Trong cuộc gọi: tắt mic, tắt camera, chuyển thoại ↔ video, chia sẻ màn hình (P1+), đổi thiết bị mic/loa.
- Kết thúc: ghi **tin nhắn lịch sử cuộc gọi** vào cuộc trò chuyện (thời lượng, gọi nhỡ).
- Đổ chuông trên tất cả thiết bị đang đăng nhập; nghe ở một thiết bị → các thiết bị khác ngừng đổ chuông.
- Chất lượng: tự giảm độ phân giải khi mạng yếu; hiển thị cảnh báo mạng yếu.

### M10. Gọi nhóm / Họp nhóm (P2)
- Cần **SFU** (media server, vd. LiveKit, mediasoup, Janus) – không dùng P2P mesh khi > 4 người.
- Gọi nhóm từ trong nhóm chat: MVP-P2 giới hạn ~8–16 người video, 50 người audio.
- Meeting: tạo phòng có link, lịch hẹn, phòng chờ (host duyệt), host tắt mic người khác, chia sẻ màn hình, giơ tay, chat trong phòng.
- Ghi hình cuộc họp: P2+ (chi phí lưu trữ, cần thông báo đồng ý ghi hình cho mọi người).

### M11. Quản trị & kiểm duyệt (P0 tối thiểu)
- Người dùng: **báo cáo** tin nhắn/tài khoản/nhóm (lý do: spam, lừa đảo, quấy rối, nội dung cấm).
- Trang Admin nội bộ: xem báo cáo, khoá/mở tài khoản, giải tán nhóm vi phạm, thống kê cơ bản (DAU, số tin nhắn).
- Chống spam: giới hạn tốc độ gửi (vd. 20 tin/10 giây), giới hạn tạo nhóm, giới hạn lời mời kết bạn.
- Tuân thủ pháp lý VN (Nghị định 13/2023 về bảo vệ dữ liệu cá nhân; Nghị định 147/2024 về dịch vụ internet – xác thực tài khoản bằng SĐT): cần tư vấn pháp lý trước khi ra mắt công khai.

### M12. Tiện ích nhóm (P1)
- Bình chọn (poll): một/nhiều lựa chọn, ẩn danh tuỳ chọn, hạn chót.
- Nhắc hẹn / lịch: thông báo tới thành viên đến giờ.
- Ghim tin nhắn (tối đa 3–5), ghi chú/bảng tin nhóm.

### M13. Bảo mật & quyền riêng tư
- P0: HTTPS/WSS, mã hoá dữ liệu lưu trữ (at-rest), mật khẩu hash (bcrypt/argon2).
- P1: quản lý thiết bị đăng nhập (xem & đăng xuất từ xa), xác thực 2 lớp (2FA).
- P2: **Mã hoá đầu-cuối (E2EE)** cho chat 1-1 — lưu ý: làm khó tìm kiếm phía server, đồng bộ đa thiết bị và kiểm duyệt; cần quyết định sớm vì ảnh hưởng kiến trúc.

### M14–M15. Channel/Community, Bot & API (P2)
- Channel: 1 người/nhóm admin đăng, nhiều người theo dõi; không giới hạn thành viên.
- Bot/API: webhook gửi tin vào nhóm, bot trả lời tự động — mở đường cho B2B.

### M16. Cài đặt (P0 cơ bản)
- Giao diện sáng/tối, ngôn ngữ (Việt/Anh), âm báo, quyền riêng tư (ai được nhắn/gọi/xem online).

## 5. Đề xuất phạm vi theo giai đoạn

| Giai đoạn | Nội dung | Mục tiêu |
|---|---|---|
| **MVP (Phase 1) – ĐÃ CHỐT** | M1, M2, M3, M4, M5, M6, M7 cơ bản, M11 tối thiểu, M16 cơ bản | Chat 1-1 & nhóm ổn định, realtime, đa thiết bị |
| **Phase 2** | M8, M9 (gọi thoại/video 1-1), M7 nâng cao, M12, M13 (2FA, thiết bị), sửa tin, tin nhắn thoại, sticker | Tăng tương tác, cạnh tranh trực tiếp Zalo/Messenger |
| **Phase 3** | M10 (gọi nhóm/meeting), M14, M15, E2EE, app mobile | Mở rộng quy mô & B2B |

**Quyết định 2026-09-26:** Kam chọn phương án 1, gọi thoại/video 1-1 để ở Phase 2.

~~**Phương án thay thế:** nếu gọi video là điểm khác biệt cốt lõi mà Kam muốn, có thể đưa M8–M9 vào MVP (tăng thêm ~3–4 tuần và chi phí TURN server).~~

## 6. Rủi ro & điểm cần quyết định sớm

1. **E2EE hay không?** Ảnh hưởng toàn bộ kiến trúc lưu trữ, tìm kiếm, kiểm duyệt. Khuyến nghị: không E2EE ở MVP, thiết kế để thêm sau cho 1-1.
2. **Gọi/họp:** tự dựng WebRTC + TURN/SFU hay dùng dịch vụ (Agora, Twilio, LiveKit Cloud, Stringee – VN)? Dịch vụ giúp ra nhanh nhưng tốn phí theo phút.
3. **Chi phí lưu trữ file** tăng nhanh — cần chính sách giới hạn/thời hạn.
4. **Pháp lý VN:** xác thực SĐT, lưu trữ dữ liệu tại VN, xử lý yêu cầu cơ quan chức năng.
5. **Realtime ở quy mô lớn:** WebSocket + message broker; thiết kế cho mở rộng ngang ngay từ đầu.

## 7. Bước tiếp theo đề xuất

1. Kam chốt phạm vi MVP (module + giả định ở mục 1).
2. Viết user stories & tiêu chí chấp nhận cho từng module MVP.
3. Wireframe các màn hình chính (danh sách hội thoại, khung chat, thông tin nhóm, hồ sơ).
4. Đề xuất kiến trúc kỹ thuật & tech stack, ước lượng thời gian/nhân sự, lập roadmap.
