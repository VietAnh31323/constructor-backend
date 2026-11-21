package com.backend.constructor.core.service;

import com.backend.constructor.app.dto.steel.SteelLineDto;
import com.backend.constructor.app.dto.steel.SteelOutput;
import com.backend.constructor.app.dto.steel_project.*;
import com.backend.constructor.common.error.BusinessException;
import com.backend.constructor.core.domain.entity.SteelCategoryEntity;
import com.backend.constructor.core.domain.entity.SteelCategoryLineEntity;
import com.backend.constructor.infras.repository.jpa.SteelCategoryJpaRepository;
import com.backend.constructor.infras.repository.jpa.SteelCategoryLineJpaRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SteelExcelPdfService {

    private final SteelProjectService steelProjectService;
    private final SteelCategoryJpaRepository steelCategoryJpaRepository;
    private final SteelCategoryLineJpaRepository steelCategoryLineJpaRepository;

    private static final DecimalFormat NUM_FMT = new DecimalFormat("#,##0.##");

    // ================= 3.05: EXPORT EXCEL =================
    public byte[] exportExcel(Long steelProjectId) {
        SteelProjectDto project = steelProjectService.getDetail(steelProjectId);

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle cellStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT, false);
            CellStyle centerStyle = createBodyStyle(workbook, HorizontalAlignment.CENTER, false);
            CellStyle numStyle = createNumberStyle(workbook, false);
            CellStyle boldNumStyle = createNumberStyle(workbook, true);
            CellStyle subtotalStyle = createSubtotalStyle(workbook);

            Sheet sheet = workbook.createSheet("Bang thong ke thep");
            int rowIdx = 0;

            Row titleRow = sheet.createRow(rowIdx++);
            titleRow.setHeightInPoints(26);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BẢNG THỐNG KÊ CỐT THÉP: " + safeStr(project.getName()).toUpperCase());
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 14));

            rowIdx = writeMetaRow(sheet, rowIdx, "Mã bảng thép:", safeStr(project.getCode()),
                    "Dự án chính:", project.getProjectCode() != null ? project.getProjectCode() + " - " + safeStr(project.getProjectName()) : "—",
                    cellStyle);
            rowIdx = writeMetaRow(sheet, rowIdx, "Chủ đầu tư:", safeStr(project.getOwner()),
                    "Địa điểm:", safeStr(project.getAddress()),
                    cellStyle);
            rowIdx = writeMetaRow(sheet, rowIdx, "Tổng chiều dài (m):", NUM_FMT.format(zeroIfNull(project.getTotalLength())),
                    "Tổng khối lượng (kg):", NUM_FMT.format(zeroIfNull(project.getTotalWeight())),
                    cellStyle);
            rowIdx++;

            String[] headers = {
                    "STT", "Cấu kiện", "SL CK", "Số hiệu", "Hình dạng",
                    "Ø (mm)", "Kích thước các đoạn (mm)", "Nối chồng (mm)", "Trừ uốn (mm)",
                    "L cắt (m)", "SL/1 CK", "Tổng thanh", "Tổng dài (m)", "Khối lượng (kg)", "Ghi chú"
            };
            Row headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            int stt = 1;
            List<SteelProjectAssemblyMapDto> assemblies = project.getSteelProjectAssemblyMaps() != null
                    ? project.getSteelProjectAssemblyMaps()
                    : Collections.emptyList();

            for (SteelProjectAssemblyMapDto assembly : assemblies) {
                List<SteelOutput> bars = assembly.getSteels() != null ? assembly.getSteels() : Collections.emptyList();
                for (SteelOutput bar : bars) {
                    Row r = sheet.createRow(rowIdx);
                    byte[] imgBytes = loadBarImageBytes(bar);
                    if (imgBytes != null) {
                        r.setHeightInPoints(38);
                    } else {
                        r.setHeightInPoints(22);
                    }

                    createNumCell(r, 0, stt++, centerStyle);
                    createCell(r, 1, safeStr(assembly.getAssemblyName()), cellStyle);
                    createNumCell(r, 2, assembly.getSameQuantity(), centerStyle);
                    createCell(r, 3, safeStr(bar.getBarLabel()), centerStyle);

                    String shapeText = bar.getSteelCategoryCode() != null
                            ? bar.getSteelCategoryCode() + (bar.getSteelCategoryName() != null ? " - " + bar.getSteelCategoryName() : "")
                            : "";
                    createCell(r, 4, imgBytes == null ? shapeText : "", cellStyle);
                    if (imgBytes != null) {
                        embedExcelImage(workbook, drawing, imgBytes, 4, rowIdx);
                    }

                    createNumCell(r, 5, bar.getBarDiameter(), centerStyle);
                    createCell(r, 6, formatSteelLines(bar.getSteelLines()), cellStyle);
                    createNumCell(r, 7, bar.getSpliceLength(), numStyle);
                    createNumCell(r, 8, bar.getBendDeduction(), numStyle);
                    createNumCell(r, 9, bar.getCutLengthMeter(), numStyle);
                    createNumCell(r, 10, bar.getBarQuantity(), centerStyle);
                    createNumCell(r, 11, bar.getTotalBars(), centerStyle);
                    createNumCell(r, 12, bar.getTotalLength(), numStyle);
                    createNumCell(r, 13, bar.getTotalWeight(), boldNumStyle);
                    createCell(r, 14, safeStr(bar.getNote()), cellStyle);
                    rowIdx++;
                }

                Row subRow = sheet.createRow(rowIdx++);
                subRow.setHeightInPoints(22);
                for (int i = 0; i < headers.length; i++) {
                    subRow.createCell(i).setCellStyle(subtotalStyle);
                }
                subRow.getCell(1).setCellValue("Cộng cấu kiện: " + safeStr(assembly.getAssemblyName()));
                subRow.getCell(12).setCellValue(zeroIfNull(assembly.getTotalLength()).doubleValue());
                subRow.getCell(12).setCellStyle(boldNumStyle);
                subRow.getCell(13).setCellValue(zeroIfNull(assembly.getTotalWeight()).doubleValue());
                subRow.getCell(13).setCellStyle(boldNumStyle);
            }

            Row grandRow = sheet.createRow(rowIdx);
            grandRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                grandRow.createCell(i).setCellStyle(subtotalStyle);
            }
            grandRow.getCell(1).setCellValue("TỔNG CỘNG TOÀN BẢNG");
            grandRow.getCell(12).setCellValue(zeroIfNull(project.getTotalLength()).doubleValue());
            grandRow.getCell(12).setCellStyle(boldNumStyle);
            grandRow.getCell(13).setCellValue(zeroIfNull(project.getTotalWeight()).doubleValue());
            grandRow.getCell(13).setCellStyle(boldNumStyle);

            int[] widths = {6, 22, 8, 10, 18, 9, 28, 13, 12, 11, 9, 11, 14, 15, 20};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // Sheet 2: Tổng hợp theo Ø
            Sheet sumSheet = workbook.createSheet("Tong hop theo duong kinh");
            int sr = 0;
            Row sTitle = sumSheet.createRow(sr++);
            Cell sTitleCell = sTitle.createCell(0);
            sTitleCell.setCellValue("BẢNG TỔNG HỢP THÉP THEO ĐƯỜNG KÍNH (Ø)");
            sTitleCell.setCellStyle(titleStyle);
            sumSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));
            sr++;

            String[] sumHeaders = {"Đường kính (Ø mm)", "Tổng số thanh", "Tổng chiều dài (m)", "Tổng khối lượng (kg)", "Quy đổi (tấn)"};
            Row sHeader = sumSheet.createRow(sr++);
            sHeader.setHeightInPoints(26);
            for (int i = 0; i < sumHeaders.length; i++) {
                Cell c = sHeader.createCell(i);
                c.setCellValue(sumHeaders[i]);
                c.setCellStyle(headerStyle);
            }

            List<DiameterSummaryDto> summaries = project.getDiameterSummaries() != null
                    ? project.getDiameterSummaries()
                    : Collections.emptyList();
            for (DiameterSummaryDto d : summaries) {
                Row r = sumSheet.createRow(sr++);
                createCell(r, 0, "Ø" + NUM_FMT.format(zeroIfNull(d.getBarDiameter())), centerStyle);
                createNumCell(r, 1, d.getTotalBars(), centerStyle);
                createNumCell(r, 2, d.getTotalLengthMeter(), numStyle);
                createNumCell(r, 3, d.getTotalWeightKg(), boldNumStyle);
                createNumCell(r, 4, zeroIfNull(d.getTotalWeightKg()).divide(BigDecimal.valueOf(1000)), numStyle);
            }
            for (int i = 0; i < sumHeaders.length; i++) {
                sumSheet.setColumnWidth(i, 22 * 256);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Export Excel error", e);
            throw BusinessException.exception("STL001", "Không thể xuất file Excel thống kê thép: " + e.getMessage());
        }
    }

    // ================= 3.05: EXPORT PDF =================
    public byte[] exportPdf(Long steelProjectId) {
        SteelProjectDto project = steelProjectService.getDetail(steelProjectId);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 20, 20, 22, 22);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bf = loadVietnameseBaseFont();
            Font titleFont = new Font(bf, 14, Font.BOLD);
            Font subTitleFont = new Font(bf, 10, Font.BOLD);
            Font normalFont = new Font(bf, 8.5f, Font.NORMAL);
            Font boldFont = new Font(bf, 8.5f, Font.BOLD);
            Font headerFont = new Font(bf, 8.5f, Font.BOLD, BaseColor.WHITE);

            Paragraph title = new Paragraph("BẢNG THỐNG KÊ CỐT THÉP", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);

            Paragraph nameP = new Paragraph(safeStr(project.getName()), subTitleFont);
            nameP.setAlignment(Element.ALIGN_CENTER);
            nameP.setSpacingAfter(8f);
            doc.add(nameP);

            PdfPTable meta = new PdfPTable(4);
            meta.setWidthPercentage(100);
            meta.setWidths(new float[]{1.4f, 2.6f, 1.4f, 2.6f});
            addMetaCell(meta, "Mã bảng thép:", boldFont);
            addMetaCell(meta, safeStr(project.getCode()), normalFont);
            addMetaCell(meta, "Dự án chính:", boldFont);
            addMetaCell(meta, project.getProjectCode() != null ? project.getProjectCode() + " - " + safeStr(project.getProjectName()) : "—", normalFont);
            addMetaCell(meta, "Chủ đầu tư:", boldFont);
            addMetaCell(meta, safeStr(project.getOwner()), normalFont);
            addMetaCell(meta, "Địa điểm:", boldFont);
            addMetaCell(meta, safeStr(project.getAddress()), normalFont);
            meta.setSpacingAfter(10f);
            doc.add(meta);

            float[] colWidths = {0.7f, 2.2f, 0.8f, 0.9f, 2.0f, 0.9f, 2.8f, 1.0f, 1.0f, 1.0f, 0.9f, 1.0f, 1.3f, 1.4f};
            PdfPTable table = new PdfPTable(colWidths.length);
            table.setWidthPercentage(100);
            table.setWidths(colWidths);
            table.setHeaderRows(1);

            String[] headers = {
                    "STT", "Cấu kiện", "SL CK", "Số hiệu", "Hình dạng",
                    "Ø", "Kích thước đoạn (mm)", "Nối", "Trừ uốn",
                    "L cắt (m)", "SL/CK", "Tổng SL", "Dài (m)", "KL (kg)"
            };
            BaseColor headerBg = new BaseColor(30, 64, 175);
            for (String h : headers) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setBackgroundColor(headerBg);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setVerticalAlignment(Element.ALIGN_MIDDLE);
                c.setPadding(5f);
                table.addCell(c);
            }

            int stt = 1;
            List<SteelProjectAssemblyMapDto> assemblies = project.getSteelProjectAssemblyMaps() != null
                    ? project.getSteelProjectAssemblyMaps()
                    : Collections.emptyList();

            for (SteelProjectAssemblyMapDto assembly : assemblies) {
                List<SteelOutput> bars = assembly.getSteels() != null ? assembly.getSteels() : Collections.emptyList();
                for (SteelOutput bar : bars) {
                    addPdfCell(table, String.valueOf(stt++), normalFont, Element.ALIGN_CENTER);
                    addPdfCell(table, safeStr(assembly.getAssemblyName()), normalFont, Element.ALIGN_LEFT);
                    addPdfCell(table, String.valueOf(assembly.getSameQuantity() != null ? assembly.getSameQuantity() : 1), normalFont, Element.ALIGN_CENTER);
                    addPdfCell(table, safeStr(bar.getBarLabel()), normalFont, Element.ALIGN_CENTER);

                    byte[] imgBytes = loadBarImageBytes(bar);
                    if (imgBytes != null) {
                        try {
                            Image img = Image.getInstance(imgBytes);
                            img.scaleToFit(70f, 26f);
                            PdfPCell imgCell = new PdfPCell(img, false);
                            imgCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            imgCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            imgCell.setPadding(3f);
                            table.addCell(imgCell);
                        } catch (Exception ex) {
                            addPdfCell(table, safeStr(bar.getSteelCategoryCode()), normalFont, Element.ALIGN_CENTER);
                        }
                    } else {
                        String label = bar.getSteelCategoryCode() != null ? bar.getSteelCategoryCode() : "Thẳng";
                        addPdfCell(table, label, normalFont, Element.ALIGN_CENTER);
                    }

                    addPdfCell(table, "Ø" + NUM_FMT.format(zeroIfNull(bar.getBarDiameter())), normalFont, Element.ALIGN_CENTER);
                    addPdfCell(table, formatSteelLines(bar.getSteelLines()), normalFont, Element.ALIGN_LEFT);
                    addPdfCell(table, NUM_FMT.format(zeroIfNull(bar.getSpliceLength())), normalFont, Element.ALIGN_RIGHT);
                    addPdfCell(table, NUM_FMT.format(zeroIfNull(bar.getBendDeduction())), normalFont, Element.ALIGN_RIGHT);
                    addPdfCell(table, NUM_FMT.format(zeroIfNull(bar.getCutLengthMeter())), normalFont, Element.ALIGN_RIGHT);
                    addPdfCell(table, String.valueOf(bar.getBarQuantity() != null ? bar.getBarQuantity() : 0), normalFont, Element.ALIGN_CENTER);
                    addPdfCell(table, String.valueOf(bar.getTotalBars() != null ? bar.getTotalBars() : 0), normalFont, Element.ALIGN_CENTER);
                    addPdfCell(table, NUM_FMT.format(zeroIfNull(bar.getTotalLength())), normalFont, Element.ALIGN_RIGHT);
                    addPdfCell(table, NUM_FMT.format(zeroIfNull(bar.getTotalWeight())), boldFont, Element.ALIGN_RIGHT);
                }

                PdfPCell subLabel = new PdfPCell(new Phrase("Cộng cấu kiện: " + safeStr(assembly.getAssemblyName()), boldFont));
                subLabel.setColspan(12);
                subLabel.setBackgroundColor(new BaseColor(241, 245, 249));
                subLabel.setPadding(4f);
                table.addCell(subLabel);
                addPdfBgCell(table, NUM_FMT.format(zeroIfNull(assembly.getTotalLength())), boldFont, Element.ALIGN_RIGHT, new BaseColor(241, 245, 249));
                addPdfBgCell(table, NUM_FMT.format(zeroIfNull(assembly.getTotalWeight())), boldFont, Element.ALIGN_RIGHT, new BaseColor(241, 245, 249));
            }

            PdfPCell grandLabel = new PdfPCell(new Phrase("TỔNG CỘNG TOÀN BẢNG", boldFont));
            grandLabel.setColspan(12);
            grandLabel.setBackgroundColor(new BaseColor(254, 243, 199));
            grandLabel.setPadding(5f);
            table.addCell(grandLabel);
            addPdfBgCell(table, NUM_FMT.format(zeroIfNull(project.getTotalLength())), boldFont, Element.ALIGN_RIGHT, new BaseColor(254, 243, 199));
            addPdfBgCell(table, NUM_FMT.format(zeroIfNull(project.getTotalWeight())), boldFont, Element.ALIGN_RIGHT, new BaseColor(254, 243, 199));

            table.setSpacingAfter(12f);
            doc.add(table);

            // Bảng tổng hợp theo Ø
            Paragraph sumTitle = new Paragraph("TỔNG HỢP KHỐI LƯỢNG THEO ĐƯỜNG KÍNH (Ø)", subTitleFont);
            sumTitle.setSpacingAfter(5f);
            doc.add(sumTitle);

            PdfPTable sumTable = new PdfPTable(5);
            sumTable.setWidthPercentage(65);
            sumTable.setHorizontalAlignment(Element.ALIGN_LEFT);
            String[] sHeaders = {"Đường kính", "Tổng số thanh", "Tổng chiều dài (m)", "Tổng khối lượng (kg)", "Quy đổi (tấn)"};
            for (String h : sHeaders) {
                PdfPCell c = new PdfPCell(new Phrase(h, headerFont));
                c.setBackgroundColor(headerBg);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(4f);
                sumTable.addCell(c);
            }
            List<DiameterSummaryDto> summaries = project.getDiameterSummaries() != null
                    ? project.getDiameterSummaries()
                    : Collections.emptyList();
            for (DiameterSummaryDto d : summaries) {
                addPdfCell(sumTable, "Ø" + NUM_FMT.format(zeroIfNull(d.getBarDiameter())), boldFont, Element.ALIGN_CENTER);
                addPdfCell(sumTable, String.valueOf(d.getTotalBars()), normalFont, Element.ALIGN_RIGHT);
                addPdfCell(sumTable, NUM_FMT.format(zeroIfNull(d.getTotalLengthMeter())), normalFont, Element.ALIGN_RIGHT);
                addPdfCell(sumTable, NUM_FMT.format(zeroIfNull(d.getTotalWeightKg())), boldFont, Element.ALIGN_RIGHT);
                addPdfCell(sumTable, NUM_FMT.format(zeroIfNull(d.getTotalWeightKg()).divide(BigDecimal.valueOf(1000))), normalFont, Element.ALIGN_RIGHT);
            }
            doc.add(sumTable);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Export PDF error", e);
            throw BusinessException.exception("STL002", "Không thể xuất file PDF thống kê thép: " + e.getMessage());
        }
    }

    // ================= 3.06: TEMPLATE IMPORT EXCEL =================
    public byte[] exportImportTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook, HorizontalAlignment.LEFT, false);

            Sheet sheet = workbook.createSheet("DuLieuThep");
            String[] cols = {
                    "Tên cấu kiện (*)",
                    "SL cấu kiện (*)",
                    "Số hiệu thanh (*)",
                    "Mã hình dạng",
                    "Đường kính Ø (mm) (*)",
                    "Kích thước các đoạn (mm) (*)",
                    "Nối chồng (mm)",
                    "Trừ uốn (mm)",
                    "SL thanh / 1 CK (*)",
                    "Ghi chú"
            };
            Row header = sheet.createRow(0);
            header.setHeightInPoints(26);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            List<SteelCategoryEntity> categories = steelCategoryJpaRepository.findAll();
            String sampleCatCode = categories.isEmpty() ? "" : safeStr(categories.get(0).getCode());

            Object[][] samples = {
                    {"Móng M1", 4, "1", sampleCatCode, 16, "L1=300;L2=2400;L3=300", 0, 0, 8, "Thép lớp dưới"},
                    {"Dầm D1", 2, "2a", "", 20, "L=6200", 600, 0, 4, "Thép chủ dầm"}
            };
            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    Cell cell = row.createCell(c);
                    Object val = samples[r][c];
                    if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(String.valueOf(val));
                    }
                    cell.setCellStyle(bodyStyle);
                }
            }
            int[] widths = {24, 14, 16, 16, 20, 32, 16, 14, 18, 24};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // Sheet 2: Danh mục hình dạng để user tra cứu
            Sheet catSheet = workbook.createSheet("DanhMucHinhDang");
            Row ch = catSheet.createRow(0);
            String[] cCols = {"Mã hình dạng", "Tên hình dạng", "Các đoạn tham số"};
            for (int i = 0; i < cCols.length; i++) {
                Cell cell = ch.createCell(i);
                cell.setCellValue(cCols[i]);
                cell.setCellStyle(headerStyle);
            }
            int cr = 1;
            for (SteelCategoryEntity cat : categories) {
                Row row = catSheet.createRow(cr++);
                List<SteelCategoryLineEntity> lines = steelCategoryLineJpaRepository.findAllBySteelCategoryId(cat.getId());
                String params = lines.stream().map(SteelCategoryLineEntity::getParamName).collect(Collectors.joining(", "));
                createCell(row, 0, safeStr(cat.getCode()), bodyStyle);
                createCell(row, 1, safeStr(cat.getName()), bodyStyle);
                createCell(row, 2, params, bodyStyle);
            }
            catSheet.setColumnWidth(0, 20 * 256);
            catSheet.setColumnWidth(1, 30 * 256);
            catSheet.setColumnWidth(2, 35 * 256);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Export template error", e);
            throw BusinessException.exception("STL003", "Không thể tạo file mẫu Excel: " + e.getMessage());
        }
    }

    // ================= 3.06: IMPORT EXCEL WITH VALIDATION =================
    @Transactional
    public SteelImportResultDto importExcel(Long steelProjectId, MultipartFile file, boolean replaceExisting) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.exception("STL004", "Vui lòng chọn file Excel để nhập dữ liệu");
        }
        SteelProjectDto existingProject = steelProjectService.getDetail(steelProjectId);

        Map<String, SteelCategoryEntity> categoryByCode = steelCategoryJpaRepository.findAll().stream()
                .filter(c -> StringUtils.isNotBlank(c.getCode()))
                .collect(Collectors.toMap(c -> c.getCode().trim().toUpperCase(), c -> c, (a, b) -> a));

        List<SteelImportErrorDto> errors = new ArrayList<>();
        Map<String, SteelProjectAssemblyMapDto> groupedAssemblies = new LinkedHashMap<>();
        int totalRows = 0;
        int validRows = 0;

        DataFormatter formatter = new DataFormatter();
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            for (int r = 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowBlank(row, formatter)) {
                    continue;
                }
                totalRows++;
                int excelRow = r + 1;
                List<SteelImportErrorDto> rowErrors = new ArrayList<>();

                String assemblyName = readString(row.getCell(0), formatter);
                if (StringUtils.isBlank(assemblyName)) {
                    rowErrors.add(err(excelRow, "Tên cấu kiện", assemblyName, "Tên cấu kiện không được để trống"));
                }

                Long sameQty = parsePositiveLong(row.getCell(1), formatter, excelRow, "SL cấu kiện", true, rowErrors);
                String barLabel = readString(row.getCell(2), formatter);
                if (StringUtils.isBlank(barLabel)) {
                    rowErrors.add(err(excelRow, "Số hiệu thanh", barLabel, "Số hiệu thanh không được để trống"));
                }

                String catCode = readString(row.getCell(3), formatter);
                SteelCategoryEntity matchedCat = null;
                if (StringUtils.isNotBlank(catCode)) {
                    matchedCat = categoryByCode.get(catCode.trim().toUpperCase());
                    if (matchedCat == null) {
                        rowErrors.add(err(excelRow, "Mã hình dạng", catCode, "Không tìm thấy mã hình dạng trong danh mục"));
                    }
                }

                BigDecimal diameter = parseDecimal(row.getCell(4), formatter, excelRow, "Đường kính Ø (mm)", true, false, rowErrors);
                if (diameter != null && diameter.compareTo(BigDecimal.ZERO) <= 0) {
                    rowErrors.add(err(excelRow, "Đường kính Ø (mm)", diameter.toPlainString(), "Đường kính phải lớn hơn 0"));
                }

                String segmentsRaw = readString(row.getCell(5), formatter);
                List<SteelLineDto> steelLines = parseSteelSegments(segmentsRaw, excelRow, rowErrors);

                BigDecimal splice = parseDecimal(row.getCell(6), formatter, excelRow, "Nối chồng (mm)", false, true, rowErrors);
                BigDecimal bendDeduction = parseDecimal(row.getCell(7), formatter, excelRow, "Trừ uốn (mm)", false, true, rowErrors);
                Long barQty = parsePositiveLong(row.getCell(8), formatter, excelRow, "SL thanh / 1 CK", true, rowErrors);
                String note = readString(row.getCell(9), formatter);

                if (!rowErrors.isEmpty()) {
                    errors.addAll(rowErrors);
                    continue;
                }

                validRows++;
                String groupKey = assemblyName.trim().toLowerCase() + "::" + sameQty;
                SteelProjectAssemblyMapDto assembly = groupedAssemblies.computeIfAbsent(groupKey, k ->
                        SteelProjectAssemblyMapDto.builder()
                                .assemblyName(assemblyName.trim())
                                .sameQuantity(sameQty)
                                .steels(new ArrayList<>())
                                .build());

                SteelOutput bar = SteelOutput.builder()
                        .barLabel(barLabel.trim())
                        .steelCategoryId(matchedCat != null ? matchedCat.getId() : null)
                        .images(matchedCat != null ? matchedCat.getImages() : null)
                        .barDiameter(diameter)
                        .spliceLength(splice != null ? splice : BigDecimal.ZERO)
                        .bendDeduction(bendDeduction != null ? bendDeduction : BigDecimal.ZERO)
                        .barQuantity(barQty)
                        .note(StringUtils.trimToNull(note))
                        .steelLines(steelLines)
                        .build();
                assembly.getSteels().add(bar);
            }
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("Import Excel error", e);
            throw BusinessException.exception("STL005", "File Excel không hợp lệ hoặc bị lỗi định dạng: " + e.getMessage());
        }

        if (totalRows == 0) {
            errors.add(err(2, "Tệp Excel", "", "File không có dòng dữ liệu nào để nhập"));
        }

        if (!errors.isEmpty()) {
            return SteelImportResultDto.builder()
                    .success(false)
                    .totalRows(totalRows)
                    .validRows(validRows)
                    .errorCount(errors.size())
                    .importedAssemblyCount(0)
                    .importedBarCount(0)
                    .errors(errors)
                    .build();
        }

        List<SteelProjectAssemblyMapDto> importedAssemblies = new ArrayList<>(groupedAssemblies.values());
        List<SteelProjectAssemblyMapDto> finalAssemblies = new ArrayList<>();
        if (!replaceExisting && existingProject.getSteelProjectAssemblyMaps() != null) {
            finalAssemblies.addAll(existingProject.getSteelProjectAssemblyMaps());
        }
        finalAssemblies.addAll(importedAssemblies);
        existingProject.setSteelProjectAssemblyMaps(finalAssemblies);
        steelProjectService.update(existingProject);
        SteelProjectDto updated = steelProjectService.getDetail(steelProjectId);

        return SteelImportResultDto.builder()
                .success(true)
                .totalRows(totalRows)
                .validRows(validRows)
                .errorCount(0)
                .importedAssemblyCount(importedAssemblies.size())
                .importedBarCount(validRows)
                .errors(Collections.emptyList())
                .steelProject(updated)
                .build();
    }

    // ================= HELPERS =================

    private List<SteelLineDto> parseSteelSegments(String raw, int excelRow, List<SteelImportErrorDto> errors) {
        if (StringUtils.isBlank(raw)) {
            errors.add(err(excelRow, "Kích thước các đoạn (mm)", raw, "Kích thước các đoạn không được để trống"));
            return Collections.emptyList();
        }
        String[] tokens = raw.split("[;+,|]");
        List<SteelLineDto> lines = new ArrayList<>();
        int idx = 1;
        for (String token : tokens) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            String paramName;
            String numStr;
            if (t.contains("=")) {
                String[] kv = t.split("=", 2);
                paramName = kv[0].trim();
                numStr = kv[1].trim();
            } else {
                paramName = "L" + idx;
                numStr = t;
            }
            if (StringUtils.isBlank(paramName)) {
                paramName = "L" + idx;
            }
            try {
                BigDecimal val = new BigDecimal(numStr.replace(",", "."));
                if (val.compareTo(BigDecimal.ZERO) < 0) {
                    errors.add(err(excelRow, "Kích thước các đoạn (mm)", t, "Giá trị đoạn không được âm"));
                } else {
                    lines.add(SteelLineDto.builder().paramName(paramName).value(val).build());
                }
            } catch (NumberFormatException ex) {
                errors.add(err(excelRow, "Kích thước các đoạn (mm)", t, "Định dạng kích thước không hợp lệ (VD: L1=500;L2=5400)"));
            }
            idx++;
        }
        if (lines.isEmpty() && errors.stream().noneMatch(e -> e.getRowNumber() == excelRow && e.getColumn().startsWith("Kích thước"))) {
            errors.add(err(excelRow, "Kích thước các đoạn (mm)", raw, "Phải có ít nhất 1 đoạn kích thước hợp lệ"));
        }
        return lines;
    }

    private Long parsePositiveLong(Cell cell, DataFormatter fmt, int row, String col, boolean required, List<SteelImportErrorDto> errors) {
        String raw = readString(cell, fmt);
        if (StringUtils.isBlank(raw)) {
            if (required) {
                errors.add(err(row, col, raw, col + " không được để trống"));
            }
            return null;
        }
        try {
            BigDecimal bd = new BigDecimal(raw.replace(",", "."));
            long val = bd.longValue();
            if (val <= 0) {
                errors.add(err(row, col, raw, col + " phải lớn hơn 0"));
                return null;
            }
            return val;
        } catch (Exception e) {
            errors.add(err(row, col, raw, col + " phải là số nguyên dương"));
            return null;
        }
    }

    private BigDecimal parseDecimal(Cell cell, DataFormatter fmt, int row, String col, boolean required, boolean allowZero, List<SteelImportErrorDto> errors) {
        String raw = readString(cell, fmt);
        if (StringUtils.isBlank(raw)) {
            if (required) {
                errors.add(err(row, col, raw, col + " không được để trống"));
            }
            return allowZero ? BigDecimal.ZERO : null;
        }
        try {
            BigDecimal val = new BigDecimal(raw.replace(",", "."));
            if (val.compareTo(BigDecimal.ZERO) < 0) {
                errors.add(err(row, col, raw, col + " không được nhỏ hơn 0"));
                return null;
            }
            return val;
        } catch (Exception e) {
            errors.add(err(row, col, raw, col + " phải là giá trị số"));
            return null;
        }
    }

    private String readString(Cell cell, DataFormatter fmt) {
        if (cell == null) return "";
        return StringUtils.trimToEmpty(fmt.formatCellValue(cell));
    }

    private boolean isRowBlank(Row row, DataFormatter fmt) {
        for (int c = 0; c <= 9; c++) {
            if (StringUtils.isNotBlank(readString(row.getCell(c), fmt))) {
                return false;
            }
        }
        return true;
    }

    private SteelImportErrorDto err(int row, String col, String raw, String msg) {
        return SteelImportErrorDto.builder()
                .rowNumber(row)
                .column(col)
                .rawValue(raw)
                .message(msg)
                .build();
    }

    private byte[] loadBarImageBytes(SteelOutput bar) {
        if (bar.getImages() == null || bar.getImages().isEmpty()) {
            return null;
        }
        String url = bar.getImages().get(0).getUrl();
        if (StringUtils.isBlank(url)) {
            return null;
        }
        try {
            if (url.startsWith("data:image/")) {
                int comma = url.indexOf(',');
                if (comma > 0) {
                    return Base64.getDecoder().decode(url.substring(comma + 1));
                }
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
                conn.setConnectTimeout(1500);
                conn.setReadTimeout(2000);
                try (InputStream is = conn.getInputStream()) {
                    return is.readAllBytes();
                }
            }
            Path p = Path.of(url);
            if (Files.exists(p)) {
                return Files.readAllBytes(p);
            }
        } catch (Exception ex) {
            log.debug("Could not load bar image {}: {}", url, ex.getMessage());
        }
        return null;
    }

    private void embedExcelImage(XSSFWorkbook wb, XSSFDrawing drawing, byte[] imgBytes, int col, int row) {
        try {
            int picType = Workbook.PICTURE_TYPE_PNG;
            int idx = wb.addPicture(imgBytes, picType);
            XSSFClientAnchor anchor = new XSSFClientAnchor(50000, 30000, -50000, -30000, col, row, col + 1, row + 1);
            anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
            drawing.createPicture(anchor, idx);
        } catch (Exception ex) {
            log.debug("Embed image failed: {}", ex.getMessage());
        }
    }

    private BaseFont loadVietnameseBaseFont() throws Exception {
        String[] candidates = {
                "C:\\Windows\\Fonts\\arial.ttf",
                "C:\\Windows\\Fonts\\times.ttf",
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
        };
        for (String path : candidates) {
            if (new File(path).exists()) {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            }
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
    }

    private String formatSteelLines(List<SteelLineDto> lines) {
        if (lines == null || lines.isEmpty()) return "";
        return lines.stream()
                .map(l -> l.getParamName() + "=" + NUM_FMT.format(zeroIfNull(l.getValue())))
                .collect(Collectors.joining("; "));
    }

    private BigDecimal zeroIfNull(BigDecimal val) {
        return val != null ? val : BigDecimal.ZERO;
    }

    private String safeStr(String s) {
        return s != null ? s : "";
    }

    private int writeMetaRow(Sheet sheet, int rowIdx, String k1, String v1, String k2, String v2, CellStyle style) {
        Row r = sheet.createRow(rowIdx++);
        createCell(r, 0, k1, style);
        createCell(r, 1, v1, style);
        createCell(r, 4, k2, style);
        createCell(r, 5, v2, style);
        return rowIdx;
    }

    private void createCell(Row row, int col, String val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val);
        c.setCellStyle(style);
    }

    private void createNumCell(Row row, int col, Number val, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(val != null ? val.doubleValue() : 0d);
        c.setCellStyle(style);
    }

    private CellStyle createTitleStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setFontHeightInPoints((short) 14);
        s.setFont(f);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        return s;
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(true);
        f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        s.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.setAlignment(HorizontalAlignment.CENTER);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setAllBorders(s);
        return s;
    }

    private CellStyle createBodyStyle(Workbook wb, HorizontalAlignment align, boolean bold) {
        CellStyle s = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font f = wb.createFont();
        f.setBold(bold);
        s.setFont(f);
        s.setAlignment(align);
        s.setVerticalAlignment(VerticalAlignment.CENTER);
        s.setWrapText(true);
        setAllBorders(s);
        return s;
    }

    private CellStyle createNumberStyle(Workbook wb, boolean bold) {
        CellStyle s = createBodyStyle(wb, HorizontalAlignment.RIGHT, bold);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.##"));
        return s;
    }

    private CellStyle createSubtotalStyle(Workbook wb) {
        CellStyle s = createBodyStyle(wb, HorizontalAlignment.LEFT, true);
        s.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return s;
    }

    private void setAllBorders(CellStyle s) {
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
    }

    private void addMetaCell(PdfPTable table, String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3f);
        table.addCell(c);
    }

    private void addPdfCell(PdfPTable table, String text, Font font, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(4f);
        table.addCell(c);
    }

    private void addPdfBgCell(PdfPTable table, String text, Font font, int align, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setBackgroundColor(bg);
        c.setPadding(4f);
        table.addCell(c);
    }
}
