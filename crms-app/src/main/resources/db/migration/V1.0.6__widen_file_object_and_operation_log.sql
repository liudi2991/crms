-- 修复：上传 XLSX/DOCX/PPTX 等附件时 file_object.content_type 截断报 SYS-500
--   原因：V1.0.0 中 content_type VARCHAR(64) 不足以容纳 OOXML MIME（如
--         application/vnd.openxmlformats-officedocument.spreadsheetml.sheet 65 字节）
-- 顺带：operation_log.error_message VARCHAR(500) 太短，业务异常的堆栈摘要被截断
--   后导致"记录失败原因"自身写库失败，干扰排障

ALTER TABLE file_object   MODIFY COLUMN content_type  VARCHAR(255);
ALTER TABLE operation_log MODIFY COLUMN error_message TEXT;
