# Phân tích Kỹ thuật Bài 3: Cơ chế ThreadLocal và Rủi ro rò rỉ bộ nhớ (Memory Leak)

## Cơ chế của MDC (Mapped Diagnostic Context)
MDC trong SLF4J được cài đặt ngầm dưới dạng **ThreadLocal**. 
- `ThreadLocal` cung cấp một vùng nhớ (biến) cục bộ độc lập cho từng Thread đang chạy. 
- Nhờ vậy, khi một Web Request (được xử lý bởi một Worker Thread của Tomcat) nạp `trace_id` vào MDC, chỉ có Thread đó mới nhìn thấy và in ra `trace_id` đó. Các Request chạy song song trên các Thread khác sẽ có `trace_id` riêng của chúng, không bị nhầm lẫn với nhau.

## Rủi ro rò rỉ bộ nhớ và sai lệch Log trong Thread Pool
Các Web Server như Tomcat, Undertow sử dụng **Thread Pool** để tái sử dụng (reuse) các Thread thay vì tạo mới cho mỗi request (để tối ưu hiệu suất).
- Khi Request A hoàn tất, Thread xử lý nó không bị hủy đi mà được đưa trả lại vào Thread Pool.
- Nếu không gọi `MDC.remove()` hoặc `MDC.clear()` trong khối `finally` của Filter, giá trị `trace_id` của Request A vẫn **còn nằm kẹt lại** trong ThreadLocal của Thread đó.
- Lần tới, khi Request B (có thể không sinh ra trace_id) được cấp phát lại đúng Thread này, nó sẽ đọc được `trace_id` cũ của Request A. Kết quả là **Log của Request B bị gắn nhầm trace_id của Request A** (nhiễm chéo dữ liệu).
- Đồng thời, việc giữ lại tham chiếu vô thời hạn trong ThreadLocal có thể cản trở Garbage Collector thu hồi vùng nhớ liên quan, dẫn đến rò rỉ bộ nhớ (Memory Leak) tăng dần theo thời gian hoạt động của máy chủ.

Do đó, bắt buộc phải dọn dẹp MDC trong khối `finally` ngay khi hoàn tất request.
