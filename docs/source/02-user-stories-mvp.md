# Web Chat – User Stories & Tiêu chí chấp nhận (MVP) v0.2

_Người lập: Claude (Senior PM) · Ngày: 2026-09-26 · Phạm vi: MVP đã chốt theo phương án 1 (xem `01-phan-tich-nghiep-vu.md`)_

> **Thay đổi v0.2 (theo Kam, 2026-09-26):** người dùng là cá nhân, quy mô ~100 người; đăng ký bằng **email và tài khoản Google** (server của Kam đang xây phần này), bỏ SĐT/OTP khỏi MVP; đội 3 người, ra mắt cuối tháng 12/2026. Đã bỏ M1-07 (xoá tài khoản) và M16-02 (đa ngôn ngữ) khỏi MVP, thêm M1-08 (đăng nhập Google).
>
> **Thay đổi v0.3 (Kam, 2026-09-26):** sản phẩm song ngữ Anh/Việt → đưa M16-02 trở lại MVP (Must). Dựng i18n bằng `react-i18next` từ S1; mọi chuỗi giao diện đi qua file dịch, không viết cứng.

**Quy ước:**
- Mã story: `<Module>-<số>`, ví dụ `M3-04`.
- Độ ưu tiên trong MVP: **Must** (không có thì không ra mắt) · **Should** (nên có, có thể lùi nếu trễ).
- Ước lượng: Story point (SP) theo thang Fibonacci 1-2-3-5-8, mang tính tham khảo, sẽ tinh chỉnh cùng đội kỹ thuật.
- Tiêu chí chấp nhận (AC) viết dạng Given / When / Then khi cần.

---

## Epic M1 – Tài khoản & xác thực

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M1-01 | Là khách, tôi muốn đăng ký bằng email + mật khẩu để bắt đầu dùng ứng dụng | Must | 3 |
| M1-02 | Là khách, tôi muốn xác minh email bằng mã OTP để chứng minh tôi là chủ sở hữu | Must | 2 |
| M1-03 | Là người dùng, tôi muốn đăng nhập bằng email + mật khẩu | Must | 2 |
| M1-04 | Là người dùng, tôi muốn đặt lại mật khẩu qua email khi quên | Must | 2 |
| M1-05 | Là người dùng, tôi muốn đăng nhập trên nhiều trình duyệt cùng lúc và thấy tin nhắn đồng bộ | Must | 5 |
| M1-06 | Là người dùng, tôi muốn đăng xuất khỏi thiết bị hiện tại | Must | 1 |
| M1-08 | Là khách/người dùng, tôi muốn đăng ký và đăng nhập bằng tài khoản Google | Must | 3 |
| ~~M1-07~~ | ~~Yêu cầu xoá tài khoản~~ → để sau MVP | – | – |

_SP của M1 thấp vì phần xác thực nằm ở server Kam đang xây; SP chủ yếu là tích hợp giao diện và phiên đăng nhập._

**AC chính:**
- M1-01: Email đã tồn tại → báo "Tài khoản đã tồn tại", gợi ý đăng nhập. Mật khẩu < 8 ký tự → không cho submit.
- M1-02: OTP 6 số gửi qua email, hết hạn sau 5 phút; gửi lại tối đa 5 lần/giờ; sai 5 lần → khoá 15 phút. Tài khoản chỉ được tạo sau khi xác thực OTP thành công (theo `AuthController` của Kam).
- M1-08: Email Google trùng tài khoản email đã có → liên kết vào tài khoản đó (sau khi đăng nhập bằng mật khẩu xác nhận), không tạo tài khoản mới. Tên và ảnh Google được dùng làm hồ sơ mặc định.
- M1-03: Sai mật khẩu 5 lần liên tiếp → khoá đăng nhập 15 phút, thông báo rõ thời gian còn lại.
- M1-04: OTP đặt lại dùng một lần; đặt lại thành công → đăng xuất mọi phiên khác.
- M1-05: Gửi tin ở trình duyệt A → hiện ở trình duyệt B trong ≤ 1 giây (mạng bình thường).

## Epic M2 – Hồ sơ & danh bạ

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M2-01 | Là người dùng, tôi muốn cập nhật tên hiển thị, ảnh đại diện, giới thiệu | Must | 3 |
| M2-02 | Là người dùng, tôi muốn tìm người khác bằng email chính xác | Must | 2 |
| M2-03 | Là người dùng, tôi muốn gửi / thu hồi lời mời kết bạn | Must | 3 |
| M2-04 | Là người dùng, tôi muốn chấp nhận / từ chối lời mời kết bạn | Must | 2 |
| M2-05 | Là người dùng, tôi muốn xem danh sách bạn bè và huỷ kết bạn | Must | 2 |
| M2-06 | Là người dùng, tôi muốn chặn / bỏ chặn một người | Must | 3 |
| M2-07 | Là người dùng, tôi muốn tin nhắn của người lạ vào mục "Tin nhắn chờ" | Should | 3 |

**AC chính:**
- M2-01: Tên hiển thị 2–50 ký tự, bắt buộc. Ảnh đại diện ≤ 5MB (jpg/png/webp), tự cắt vuông.
- M2-02: Không có kết quả gợi ý mờ/liệt kê; chỉ trả kết quả khớp chính xác và người đó không bật "ẩn khỏi tìm kiếm".
- M2-03: Tối đa 30 lời mời/ngày; vượt → báo lỗi thân thiện.
- M2-06: Người bị chặn không nhắn tin được (hiển thị "Không thể gửi tin nhắn"), không thấy online, không tìm thấy mình; không có thông báo gửi cho người bị chặn. Chặn tự động huỷ kết bạn.
- M2-07: Người lạ gửi tin → vào "Tin nhắn chờ", không đẩy thông báo; người nhận trả lời hoặc chấp nhận → chuyển vào hộp thư chính.

## Epic M3 – Nhắn tin 1-1

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M3-01 | Là người dùng, tôi muốn xem danh sách cuộc trò chuyện sắp xếp theo tin mới nhất, có số tin chưa đọc | Must | 5 |
| M3-02 | Là người dùng, tôi muốn gửi và nhận tin nhắn văn bản, emoji theo thời gian thực | Must | 8 |
| M3-03 | Là người dùng, tôi muốn thấy trạng thái tin: đang gửi / đã gửi / đã nhận / đã xem | Must | 5 |
| M3-04 | Là người dùng, tôi muốn thấy "đang soạn tin" khi người kia gõ | Should | 2 |
| M3-05 | Là người dùng, tôi muốn trả lời (trích dẫn) một tin nhắn cụ thể | Must | 3 |
| M3-06 | Là người dùng, tôi muốn thả reaction vào tin nhắn | Must | 3 |
| M3-07 | Là người dùng, tôi muốn thu hồi tin nhắn đã gửi trong 24 giờ | Must | 3 |
| M3-08 | Là người dùng, tôi muốn xoá tin nhắn ở phía tôi | Should | 2 |
| M3-09 | Là người dùng, tôi muốn chuyển tiếp tin nhắn sang cuộc trò chuyện khác | Should | 3 |
| M3-10 | Là người dùng, tôi muốn ghim tin nhắn quan trọng | Should | 2 |
| M3-11 | Là người dùng, tôi muốn cuộn lên để xem lịch sử tin nhắn cũ | Must | 3 |
| M3-12 | Là người dùng, tôi muốn tin tự gửi lại khi có mạng trở lại | Must | 5 |
| M3-13 | Là người dùng, tôi muốn link trong tin nhắn có ảnh xem trước | Should | 3 |

**AC chính:**
- M3-02: Văn bản tối đa 5.000 ký tự; Enter gửi, Shift+Enter xuống dòng; độ trễ nhận ≤ 1 giây (p95) trong điều kiện bình thường.
- M3-03: "Đã xem" chỉ đánh dấu khi cuộc trò chuyện đang mở và tab đang focus.
- M3-06: Bộ 6 reaction cố định; mỗi người 1 reaction/tin, chọn lại để đổi, bấm lại để gỡ.
- M3-07: Quá 24 giờ → nút "Thu hồi" không hiển thị. Sau thu hồi, mọi phía thấy "Tin nhắn đã được thu hồi", nội dung bị xoá khỏi server.
- M3-11: Tải 30 tin/lần; giữ nguyên vị trí cuộn khi tải thêm.
- M3-12: Tin gửi lỗi hiển thị nút "Gửi lại"; hệ thống không tạo tin trùng khi gửi lại (idempotency key).

## Epic M4 – Nhóm chat

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M4-01 | Là người dùng, tôi muốn tạo nhóm với tên, ảnh và chọn thành viên từ bạn bè | Must | 5 |
| M4-02 | Là thành viên nhóm, tôi muốn nhắn tin nhóm với đủ tính năng như chat 1-1 | Must | 5 |
| M4-03 | Là trưởng/phó nhóm, tôi muốn thêm / xoá thành viên | Must | 3 |
| M4-04 | Là trưởng nhóm, tôi muốn bổ nhiệm / gỡ phó nhóm và chuyển quyền trưởng nhóm | Must | 3 |
| M4-05 | Là thành viên, tôi muốn rời nhóm | Must | 2 |
| M4-06 | Là trưởng nhóm, tôi muốn giải tán nhóm | Must | 2 |
| M4-07 | Là trưởng/phó nhóm, tôi muốn tạo link mời và thu hồi link | Should | 3 |
| M4-08 | Là trưởng/phó nhóm, tôi muốn bật "duyệt thành viên mới" | Should | 3 |
| M4-09 | Là thành viên, tôi muốn @mention một người hoặc @all | Must | 3 |
| M4-10 | Là thành viên, tôi muốn thấy tin hệ thống khi có người vào / rời / đổi tên nhóm | Must | 2 |
| M4-11 | Là thành viên, tôi muốn xem danh sách thành viên và vai trò | Must | 2 |

**AC chính:**
- M4-01: Nhóm cần ≥ 3 người (kể cả người tạo), tối đa 100. Không đặt tên → tự ghép tên 3 thành viên đầu.
- M4-03: Thành viên thường chỉ thêm người được khi nhóm bật "cho phép thành viên thêm người". Người bị xoá không xem được tin mới, vẫn giữ lịch sử cũ ở máy (chỉ đọc).
- M4-04: Owner rời nhóm → bắt buộc chọn người nhận quyền trước khi rời.
- M4-06: Giải tán cần xác nhận 2 bước; mọi thành viên nhận tin hệ thống, nhóm chuyển chỉ đọc.
- M4-09: @all chỉ Owner/Admin dùng được khi nhóm > 20 người. Người bị mention nhận thông báo kể cả khi đã tắt thông báo nhóm.
- Thành viên mới không thấy lịch sử trước thời điểm vào nhóm.

## Epic M5 – File & media

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M5-01 | Là người dùng, tôi muốn gửi ảnh (chọn, kéo thả, dán từ clipboard) | Must | 5 |
| M5-02 | Là người dùng, tôi muốn gửi file bất kỳ và thấy tiến trình tải lên | Must | 5 |
| M5-03 | Là người dùng, tôi muốn xem ảnh phóng to dạng gallery | Must | 3 |
| M5-04 | Là người dùng, tôi muốn gửi và xem video ngắn | Should | 5 |
| M5-05 | Là người dùng, tôi muốn xem kho Ảnh / File / Link của một cuộc trò chuyện | Should | 5 |

**AC chính:**
- M5-01: Gửi tối đa 20 ảnh/lần; ảnh được nén và tạo thumbnail; hiển thị thumbnail trước, ảnh gốc tải khi mở.
- M5-02: Tối đa 100MB/file; file thực thi (.exe, .bat, .msi, .apk…) bị chặn; huỷ được khi đang tải.
- M5-04: Video ≤ 100MB, phát trực tiếp trong khung chat.
- File thuộc tin nhắn bị thu hồi → bị xoá khỏi lưu trữ.

## Epic M6 – Thông báo & trạng thái

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M6-01 | Là người dùng, tôi muốn nhận thông báo trình duyệt khi có tin mới lúc không mở tab | Must | 5 |
| M6-02 | Là người dùng, tôi muốn thấy số tin chưa đọc trên tiêu đề tab và danh sách hội thoại | Must | 2 |
| M6-03 | Là người dùng, tôi muốn tắt thông báo một cuộc trò chuyện (1h / 8h / đến khi bật lại) | Must | 3 |
| M6-04 | Là người dùng, tôi muốn thấy bạn bè đang online hoặc truy cập lần cuối | Must | 3 |

**AC chính:**
- M6-01: Chỉ xin quyền thông báo sau khi người dùng đăng nhập lần đầu, có giải thích lý do. Nội dung thông báo: tên người gửi + 50 ký tự đầu.
- M6-01: Đang mở đúng cuộc trò chuyện (tab focus) → không bắn thông báo.
- M6-04: Offline > 5 phút → hiển thị "Truy cập X phút/giờ trước"; > 7 ngày → không hiển thị thời gian.

## Epic M7 – Tìm kiếm cơ bản

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M7-01 | Là người dùng, tôi muốn tìm bạn bè, nhóm, cuộc trò chuyện theo tên | Must | 3 |

**AC chính:** Hỗ trợ tìm không dấu ("nguyen" khớp "Nguyễn"); kết quả hiện trong ≤ 300ms khi gõ; tách nhóm kết quả "Bạn bè" / "Nhóm".

## Epic M11 – Kiểm duyệt tối thiểu

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M11-01 | Là người dùng, tôi muốn báo cáo một tin nhắn / tài khoản / nhóm vi phạm | Must | 3 |
| M11-02 | Là quản trị viên, tôi muốn xem danh sách báo cáo và xử lý (bỏ qua / khoá tài khoản / giải tán nhóm) | Must | 5 |
| M11-03 | Là quản trị viên, tôi muốn khoá / mở khoá tài khoản | Must | 2 |
| M11-04 | Là hệ thống, tôi muốn giới hạn tốc độ gửi tin để chống spam | Must | 3 |
| M11-05 | Là quản trị viên, tôi muốn xem số liệu cơ bản (người dùng mới, DAU, số tin/ngày) | Should | 3 |

**AC chính:**
- M11-01: Lý do: Spam, Lừa đảo, Quấy rối, Nội dung phản cảm, Khác (kèm mô tả). Báo cáo kèm ngữ cảnh 10 tin xung quanh cho admin.
- M11-03: Tài khoản bị khoá → mọi phiên bị đăng xuất, đăng nhập thấy thông báo lý do.
- M11-04: Vượt 20 tin/10 giây → tạm chặn gửi 30 giây, báo cho người dùng.
- Mọi thao tác của admin được ghi log (ai, lúc nào, làm gì).

## Epic M16 – Cài đặt cơ bản

| Mã | User story | Ưu tiên | SP |
|---|---|---|---|
| M16-01 | Là người dùng, tôi muốn chọn giao diện sáng / tối | Should | 2 |
| M16-02 | Là người dùng, tôi muốn chọn ngôn ngữ Việt / Anh | Must | 3 |
| M16-03 | Là người dùng, tôi muốn cài đặt ai được nhắn tin cho tôi (mọi người / chỉ bạn bè) | Must | 2 |
| M16-04 | Là người dùng, tôi muốn bật/tắt âm báo | Should | 1 |

---

## Tổng hợp

| Epic | Số story | Must | Should | Tổng SP |
|---|---|---|---|---|
| M1 Tài khoản | 7 | 7 | 0 | 18 |
| M2 Hồ sơ & danh bạ | 7 | 6 | 1 | 18 |
| M3 Nhắn tin 1-1 | 13 | 8 | 5 | 47 |
| M4 Nhóm chat | 11 | 9 | 2 | 33 |
| M5 File & media | 5 | 3 | 2 | 23 |
| M6 Thông báo | 4 | 4 | 0 | 13 |
| M7 Tìm kiếm | 1 | 1 | 0 | 3 |
| M11 Kiểm duyệt | 5 | 4 | 1 | 16 |
| M16 Cài đặt | 4 | 2 | 2 | 8 |
| **Tổng** | **57** | **44** | **13** | **179** |

Trong đó story **Must = 142 SP**, **Should = 37 SP**.

Ghi chú: 176 SP chưa gồm hạ tầng (CI/CD, realtime server, lưu trữ file, giám sát), thiết kế UI/UX và kiểm thử; phần này sẽ ước lượng ở bước kiến trúc.

## Câu hỏi đã được trả lời (2026-09-26)

1. Đối tượng: người dùng cá nhân, ~100 người.
2. Đăng ký: email và Google trước, chưa cần SĐT.
3. Đội: 3 người, ra mắt cuối tháng 12/2026. Lộ trình chi tiết ở `03-roadmap-mvp.md`.
