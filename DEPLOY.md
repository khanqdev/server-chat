# Deploy server-chat bằng Docker (Oracle Cloud Always Free)

Sơ đồ:

```
Trình duyệt ──► https://<app>.vercel.app          (frontend tĩnh)
            ├─► /api/*  → Vercel rewrite ─► https://API_DOMAIN/api/*  (API ở /api/v1/...)  ─► Caddy ─► Spring Boot
            └─► wss://API_DOMAIN/ws-chat/websocket (WebSocket đi thẳng) ─► Caddy ─► Spring Boot
MongoDB: Atlas M0 (MONGODB_URI)
```

## 1. Chuẩn bị VM

1. Tạo VM **Ampere A1 (ARM)** hoặc **E2.1.Micro**, image Ubuntu 22.04/24.04.
2. Mở cổng **80** và **443** (TCP, thêm 443 UDP cho HTTP/3) ở **Security List** (hoặc NSG) của VCN.
3. Mở firewall trong VM (image Ubuntu của Oracle chặn sẵn bằng iptables):

   ```bash
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
   sudo iptables -I INPUT 6 -m state --state NEW -p udp --dport 443 -j ACCEPT
   sudo netfilter-persistent save
   ```

4. Cài Docker: `curl -fsSL https://get.docker.com | sh` rồi `sudo usermod -aG docker $USER` (đăng nhập lại).

## 2. Tên miền miễn phí (DuckDNS)

1. Đăng nhập duckdns.org, tạo subdomain (vd `webchat-api`), trỏ về **IP public của VM**.
2. `API_DOMAIN=webchat-api.duckdns.org`. Caddy sẽ tự xin chứng chỉ Let's Encrypt khi khởi động (cần cổng 80/443 đã mở).

## 3. Chạy

```bash
git clone https://github.com/khanqdev/server-chat.git && cd server-chat
cp .env.example .env   # điền MONGODB_URI, JWT_SECRET, MAIL_*, GOOGLE_CLIENT_ID, ALLOWED_ORIGINS, API_DOMAIN
docker compose up -d --build
docker compose logs -f app
```

Biến quan trọng trong `.env`:

| Biến | Ghi chú |
|---|---|
| `API_DOMAIN` | Domain trỏ về VM, dùng cho HTTPS |
| `ALLOWED_ORIGINS` | Origin của frontend được mở WebSocket, vd `https://webchat.vercel.app,https://webchat-*.vercel.app` |
| `MONGODB_URI` | Atlas: thêm IP public của VM vào **Network Access** |
| `JWT_SECRET` | `openssl rand -base64 64` |

`docker-compose.yml` tự đặt `SERVER_FORWARD_HEADERS_STRATEGY=framework` (lấy IP thật từ `X-Forwarded-For` cho rate limit) và `RATE_LIMIT_SKIP_LOCALHOST=false`.

Cập nhật phiên bản mới: `git pull && docker compose up -d --build`.

## 4. Kiểm tra

```bash
curl -i https://API_DOMAIN/ws-chat/info -H "Origin: https://webchat.vercel.app"   # 200
curl -i https://API_DOMAIN/ws-chat/info -H "Origin: https://evil.example.com"     # 403
```

## Lưu ý

- **Rate limit theo IP**: Vercel không công bố dải IP cố định nên Caddy tin `X-Forwarded-For` từ mọi nguồn. Ai gọi thẳng vào `API_DOMAIN` có thể giả header này để né giới hạn theo IP; khoá theo tài khoản (sai mật khẩu/OTP 5 lần) vẫn có hiệu lực.
- **Cookie refresh** (khi chuyển sang cookie httpOnly): không đặt thuộc tính `Domain`, để trình duyệt gắn vào domain Vercel (request `/api` đi qua rewrite của Vercel).
- **Oracle thu hồi VM rảnh**: VM Always Free dùng rất ít CPU/mạng/RAM trong 7 ngày có thể bị thu hồi; nâng tài khoản lên Pay As You Go (vẫn dùng tài nguyên free) để tránh.
- RAM: JVM dùng tối đa 75% RAM container (`JAVA_TOOL_OPTIONS` trong Dockerfile). Với VM E2.1.Micro (1 GB) nên thêm swap.
