# Web Chat – Roadmap MVP (v0.4)

_Người lập: Claude (Senior PM) · Ngày: 2026-09-26 · Dựa trên `02-user-stories-mvp.md` v0.2 và `04-kien-truc-ky-thuat.md`_

> **Thay đổi v0.2:** server Java + MongoDB; cả đội làm **bán thời gian** → áp dụng phạm vi **"MVP gọn" (117 SP)** để giữ mốc cuối tháng 12, toàn bộ story Should để sau ra mắt. **Kam đã chốt MVP gọn ngày 26/09/2026.**
>
> **Thay đổi v0.4 (19/11/2026):** kiểm tra tiến độ đầu S4 cho thấy mới xong nhóm story xác thực (~13/120 SP), không có commit nào từ 04/10. **Kam chọn dời ra mắt sang cuối tháng 1/2027, giữ nguyên phạm vi MVP gọn.** Lộ trình mới ở mục 3; lộ trình cũ giữ ở mục 3b để tham khảo.
>
> **Thay đổi v0.3:** thêm song ngữ Anh/Việt (M16-02, 3 SP) → MVP gọn = **120 SP**. Frontend dùng **shadcn/ui** (Tailwind, Radix), phong cách liquid glass, có light/dark mode; font Be Vietnam Pro.

## 1. Thông số đầu vào

| Hạng mục | Giá trị |
|---|---|
| Đối tượng | Người dùng cá nhân, ~100 người |
| Đăng ký | Email + Google (server của Kam đang xây) |
| Đội | 3 người, bán thời gian |
| Tech stack | Java (Spring Boot) + MongoDB, React; chi tiết ở `04-kien-truc-ky-thuat.md` |
| Bắt đầu | Thứ Hai 28/09/2026 |
| Ra mắt | ~~Cuối tháng 12/2026~~ → **Cuối tháng 1/2027 (mục tiêu 28–31/01), trước Tết Đinh Mùi (06/02/2027)** |
| Thời gian | ~18 tuần |
| Khối lượng | **120 SP (MVP gọn, gồm song ngữ)**; 22 SP Must + 37 SP Should dời sau ra mắt |

**Giả định phân vai (cần Kam xác nhận):**
- **Người 1 – Backend (Kam):** server, xác thực, API, cơ sở dữ liệu.
- **Người 2 – Frontend:** giao diện web, trạng thái client, responsive.
- **Người 3 – Fullstack/Realtime & DevOps:** WebSocket, thông báo, lưu trữ file, triển khai, kiểm thử.

## 2. Phạm vi "MVP gọn" cho đội bán thời gian

Với ~100 người dùng cá nhân (phần lớn là người quen), các tính năng kiểm duyệt và phân quyền chi tiết ít cần ngay. Dời sau ra mắt:

| Story | Nội dung | SP | Cách xử lý tạm |
|---|---|---|---|
| M11-01, 02, 03 | Báo cáo vi phạm, trang admin, khoá tài khoản | 10 | Admin xử lý trực tiếp trong MongoDB bằng script |
| M2-06 | Chặn người dùng | 3 | Cài đặt "chỉ bạn bè được nhắn" (M16-03) thay thế |
| M4-04 | Phó nhóm, chuyển quyền trưởng nhóm | 3 | Nhóm chỉ có Trưởng nhóm + Thành viên |
| M4-09 | @mention | 3 | – |
| M6-04 | Trạng thái online / truy cập lần cuối | 3 | – |
| Toàn bộ Should | 13 story | 37 | Làm ở bản cập nhật tháng 1–2/2027 |

Còn lại **117 SP**, cộng M16-02 song ngữ (3 SP) = **120 SP**. Kiến trúc chi tiết xem `04-kien-truc-ky-thuat.md`.

## 3. Lộ trình theo sprint (v0.4, từ 19/11/2026)

**Hiện trạng 19/11:** xong M1-01, 02, 03, 04, 06, 08. Làm dở M1-05 (phiên đa trình duyệt đã chạy, chờ chat để đồng bộ tin), M16-02 (đã có i18n, chưa dịch đủ). BE có chat STOMP thử nghiệm nhưng chưa khớp contract. **Còn lại ~100 SP.**

| Sprint | Thời gian | Mục tiêu | Story | SP |
|---|---|---|---|---|
| S0, S1, S2, S3 | 28/09 – 15/11 | (đã qua) Xác thực email + Google | M1-01…04, 06, 08 | ~13 |
| **S4 – Bạn bè & nền realtime** | 16/11 – 29/11 | Hồ sơ, tìm người, kết bạn; BE chỉnh WebSocket `/ws` theo contract (`/user/queue/events`, `/user/queue/acks`), FE kết nối STOMP | M2-01…05 + việc kỹ thuật WS | 12 |
| **S5 – Nhắn tin 1-1 lõi** | 30/11 – 13/12 | Danh sách hội thoại, gửi/nhận realtime, trạng thái tin, lịch sử, gửi lại khi mất mạng; hoàn tất M1-05 | M3-01, 02, 03, 11, 12 | 26 |
| **S6 – Tương tác & tạo nhóm** | 14/12 – 27/12 | Reply, reaction, thu hồi; tạo nhóm, chat nhóm, thêm/xoá thành viên. Có Giáng sinh nên khối lượng nhẹ hơn | M3-05, 06, 07; M4-01, 02, 03 | 22 |
| **S7 – Nhóm còn lại, file, thông báo** | 28/12 – 10/01 | Rời/giải tán nhóm, tin hệ thống, danh sách thành viên; gửi ảnh, gửi file; web push, số chưa đọc; tìm kiếm | M4-05, 06, 10, 11; M5-01, 02; M6-01, 02; M7-01 | 28 |
| **S8 – Hoàn thiện, beta, ra mắt** | 11/01 – 24/01 | Tuần 1 (11–17/01): gallery, tắt thông báo, chống spam, quyền nhắn tin, dịch EN đầy đủ + bù trễ. **Đóng băng tính năng 17/01.** Tuần 2 (18–24/01): beta kín 10–20 người, sửa lỗi | M5-03; M6-03; M11-04; M16-03; M16-02 | ~12 |
| **Ra mắt** | 25/01 – 31/01 | Triển khai production, mời người dùng, theo dõi lỗi | – | – |

**Mốc quan trọng (milestone):**
- 29/11: Kết bạn chạy được; FE và BE nối được WebSocket theo contract.
- 13/12: **Demo nội bộ 1** – 2 người nhắn tin 1-1 realtime được. **Đây là điểm kiểm tra mới.**
- 10/01: **Demo nội bộ 2** – đủ luồng chat 1-1, nhóm, file, thông báo.
- 17/01: Đóng băng tính năng.
- 18/01: Beta kín.
- 28–31/01: Ra mắt.

**Điểm kiểm tra 13/12:** nếu Demo 1 chưa chạy, cắt theo thứ tự M5-03 (gallery) → M6-03 (tắt thông báo) → M4-06 (giải tán nhóm) → M7-01 (tìm kiếm), không dời ngày ra mắt thêm vì sát Tết.

## 3b. Lộ trình cũ (v0.3, không còn hiệu lực)

| Sprint | Thời gian | Mục tiêu | Story chính |
|---|---|---|---|
| **S0 – Chuẩn bị** | 28/09 – 04/10 (1 tuần) | Chốt kiến trúc, data model, API contract, wireframe; dựng repo, CI, môi trường dev/staging | – |
| **S1 – Tài khoản & bạn bè** | 05/10 – 18/10 | Đăng ký/đăng nhập email + Google, hồ sơ, kết bạn; khung giao diện chính (shadcn/ui, theme light/dark), dựng i18n `react-i18next` | M1-01…06, M1-08, M2-01…05 (30 SP) |
| **S2 – Nhắn tin 1-1 lõi** | 19/10 – 01/11 | Realtime qua STOMP, danh sách hội thoại, trạng thái tin, lịch sử, gửi lại khi mất mạng | M3-01, 02, 03, 11, 12 (26 SP) |
| **S3 – Tương tác & nhóm** | 02/11 – 15/11 | Reply, reaction, thu hồi; tạo nhóm, thêm/xoá thành viên, rời/giải tán, tin hệ thống | M3-05, 06, 07; M4-01, 02, 03, 05, 06, 10, 11 (30 SP) |
| **S4 – File, thông báo, tìm kiếm** | 16/11 – 29/11 | Gửi ảnh/file, gallery; web push, chưa đọc, tắt thông báo; tìm kiếm không dấu | M5-01…03; M6-01…03; M7-01 (26 SP) |
| **S5 – Bù trễ & hoàn thiện** | 30/11 – 13/12 | Chống spam, cài đặt quyền riêng tư; **dành phần lớn sprint để bù việc trễ** từ S1–S4. **Đóng băng tính năng 13/12** | M11-04; M16-02 (hoàn thiện bản dịch EN); M16-03 (8 SP) + phần trễ |
| **S6 – Ổn định & ra mắt** | 14/12 – 27/12 | Kiểm thử toàn diện, beta kín 10–20 người, sửa lỗi, tối ưu, triển khai production | – |
| **Ra mắt** | 28/12 – 31/12 | Publish, mời người dùng, theo dõi lỗi | – |

**Mốc quan trọng (milestone):**
- 04/10: Kiến trúc + wireframe được duyệt.
- 01/11: **Demo nội bộ 1** – 2 người nhắn tin 1-1 realtime được.
- 29/11: **Demo nội bộ 2** – đủ luồng chat 1-1, nhóm, file, thông báo.
- 13/12: Đóng băng tính năng.
- 20/12: Beta kín.
- 28–31/12: Ra mắt.

## 4. Đánh giá khả thi

**v0.4:** S4–S7 cần trung bình ~22–28 SP/sprint cho cả đội. Từ 28/09 đến 19/11 đội mới xong ~13 SP, tức nhịp hiện tại cần **tăng khoảng 4 lần**. Kế hoạch này chỉ khả thi nếu cả 3 người thật sự dành 10–15 giờ/tuần từ nay. Nếu hết S4 (29/11) mà M2-01…05 chưa xong, cần bàn lại phạm vi ngay thay vì chờ 13/12.

**v0.3 (cũ):**

- S1–S4 cần khoảng **26–30 SP/sprint cho cả đội (~9–10 SP/người mỗi 2 tuần)**; S5 là vùng đệm.
- Với nhịp bán thời gian (ước tính 10–15 giờ/người/tuần), mức này **vừa sức nhưng không dư**.
- **Điểm kiểm tra 01/11 (hết S2):** nếu đội hoàn thành dưới ~22 SP/sprint, chọn một trong hai: cắt tiếp theo thứ tự M5-03 (gallery) → M6-03 (tắt thông báo) → M4-06 (giải tán nhóm), hoặc dời ra mắt sang cuối tháng 1/2027.
- Phần xác thực đã có sẵn trên server của Kam giúp S1 nhẹ hơn ước lượng.

## 5. Rủi ro chính

| Rủi ro | Mức | Giảm thiểu |
|---|---|---|
| Realtime (WebSocket, đồng bộ đa thiết bị) phức tạp hơn dự kiến | Cao | Làm sớm ở S2, prototype ngay trong S0 |
| Giáng sinh, Tết dương lịch rơi vào S6–S7; Tết âm lịch 06/02/2027 ngay sau ra mắt | Trung bình | S6 nhẹ hơn; đóng băng 17/01, không dời ra mắt thêm |
| Đội làm bán thời gian, vận tốc thấp | **Cao** | Áp dụng MVP gọn; đo vận tốc sau S1–S2, quyết định tại điểm kiểm tra 01/11 |
| Web push không hoạt động trên Safari/iOS cũ | Thấp | Chấp nhận; hướng dẫn cài web app (PWA) trên iOS 16.4+ |
| Chi phí lưu trữ file | Thấp (100 người) | Giới hạn 100MB/file, theo dõi dung lượng hằng tháng |

## 6. Quy trình làm việc đề xuất

- Sprint 2 tuần: lập kế hoạch sprint đầu kỳ, demo + retro cuối kỳ (30–45 phút mỗi buổi, có thể gộp vào 1 buổi tối/cuối tuần). Cập nhật tiến độ bất đồng bộ qua chat 2–3 lần/tuần thay cho họp hằng ngày.
- Quản lý task trên GitHub Projects hoặc Trello; mỗi story là 1 issue, gắn mã story.
- Definition of Done: code review bởi 1 người khác, đạt tiêu chí chấp nhận, đã deploy lên staging.
