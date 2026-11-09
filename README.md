# MTrans Mobile

App dịch Tiếng Việt / Tiếng Anh / Tiếng Trung cho Android, tối ưu Samsung Galaxy Z Fold 7.

- **Văn bản:** chọn "Dịch từ" và "Dịch sang" (ngôn ngữ đã chọn ở ô đầu không có ở ô sau), gõ xong là tự dịch.
- **Camera:** chụp hoặc chọn ảnh, tự nhận ra ngôn ngữ và dịch sang Tiếng Việt; xem dạng danh sách hoặc đè lên ảnh.
- **Offline:** tải gói ngôn ngữ trong Cài đặt, mất mạng vẫn dịch được.
- **Claude:** nhập API key để dùng nút "Dịch với Claude".
- Màn trong Z Fold 7: chia đôi ảnh / bản dịch. Tự kiểm tra và cài bản mới từ GitHub Releases.

**Cài đặt:** tải file `.apk` ở mục [Releases](https://github.com/satthupc00/MTrans-Mobile/releases/latest) về điện thoại rồi mở để cài.

Phát hành bản mới: tăng `versionCode` + `versionName` trong `app/build.gradle.kts`, cập nhật `app/src/main/assets/whatsnew.json`, push. GitHub Actions tự build APK (ký bằng keystore trong Secrets `KEYSTORE_BASE64` / `KEYSTORE_PASSWORD`) và tạo Release.

Created by Mondiro
