/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.controllers;

import com.findkme.models.CodePair;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.Serializable;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletContext;
import org.primefaces.model.file.UploadedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.event.FileUploadEvent;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.CellType;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author Maverick-
 */
@Named("FindkMeController")
@RequestScoped
public class FindkMeController implements Serializable {

    private StreamedContent download;

    private UploadedFile uploadedFile;
    private String selectedFileNameMain;
    private String selectedFileNameCompare;
    private List<CodePair> listCodePair = new ArrayList<>();

    String uploadDirectory = FacesContext.getCurrentInstance().getExternalContext().getRealPath("/") + "resources/upload/";

    public UploadedFile getUploadedFile() {
        return uploadedFile;
    }

    public void setUploadedFile(UploadedFile uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    public void handleFileUpload(FileUploadEvent event) throws IOException {

        UploadedFile file = event.getFile();
        if (file != null && file.getContent() != null && file.getContent().length > 0 && file.getFileName() != null) {
            String fileName = generateUniqueFileName(file.getFileName());

            saveFile(file.getInputStream(), uploadDirectory, fileName);

            FacesMessage msg = new FacesMessage("Successful", file.getFileName() + " is uploaded.");
            FacesContext.getCurrentInstance().addMessage("message", msg);
        }
    }

    private void saveFile(InputStream inputStream, String directory, String fileName) throws IOException {

        File targetDirectory = new File(directory);

        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        File outputFile = new File(targetDirectory, fileName);

        try (OutputStream outputStream = new FileOutputStream(outputFile)) {
            int bytesRead;
            byte[] buffer = new byte[4096];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private String generateUniqueFileName(String originalFileName) {
        long timestamp = System.currentTimeMillis();
        int dotIndex = originalFileName.lastIndexOf(".");
        String extension = (dotIndex != -1) ? originalFileName.substring(dotIndex) : "";
        return "file_" + timestamp + extension;
    }

    public List<String> getFileNames() {

        File directory = new File(uploadDirectory);

        if (directory.exists() && directory.isDirectory()) {

            File[] files = directory.listFiles();

            List<String> fileNames = new ArrayList<>();

            for (File file : files) {
                if (file.isFile()) {
                    fileNames.add(file.getName());
                }
            }

            return fileNames;
        } else {
            return new ArrayList<>();
        }
    }

    public XSSFWorkbook readFileMain() {
        XSSFWorkbook workbook = null;

        System.err.println("selectedFileNameMain:" + selectedFileNameMain);

        try (FileInputStream file = new FileInputStream(uploadDirectory + selectedFileNameMain)) {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            System.err.println("Error:" + e);
        }

        return workbook;
    }

    public XSSFWorkbook readFileCompare() {
        XSSFWorkbook workbook = null;

        try (FileInputStream file = new FileInputStream(uploadDirectory + selectedFileNameCompare)) {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            this.messageOfError("Error reading file");
        }

        return workbook;
    }

    public void compare() {
        XSSFWorkbook workbookMain = readFileMain();
        XSSFWorkbook workbookCompare = readFileCompare();
        XSSFSheet sheet = workbookMain.getSheetAt(0);
        XSSFSheet sheet2 = workbookCompare.getSheetAt(0);

        XSSFWorkbook newWorkbook = new XSSFWorkbook();
        XSSFSheet newSheet = newWorkbook.createSheet("Hojas Filtradas");

        if (sheet != null && sheet2 != null) {

            int columnIndex = 1;
            int columnIndexC = 2;
            int startRow = 3;

            for (int rowNum = startRow; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                String codeNew = null;
                String codeOld = null;
                if (row != null) {
                    Cell cell = row.getCell(columnIndex);
                    if (cell != null) {
                        switch (cell.getCellTypeEnum()) {
                            case STRING:
                                String stringValue = cell.getStringCellValue();
                                int startIndex = stringValue.indexOf('-') + 1;
                                String substring = stringValue.substring(startIndex);
                                codeNew = substring.replaceAll("^0+|\\D+$", "");

                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    Date dateValue = cell.getDateCellValue();
                                    System.err.println("dateValue:" + dateValue);
                                } else {
                                    double numericValue = cell.getNumericCellValue();
                                    System.err.println("numericValue:" + numericValue);
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    Cell cellC = row.getCell(columnIndexC);
                    if (cellC != null) {
                        switch (cellC.getCellTypeEnum()) {
                            case STRING:

                                String stringValueC = cellC.getStringCellValue();
                                int startIndex = stringValueC.indexOf('-') + 1;
                                String substring = stringValueC.substring(startIndex);
                                codeOld = substring.replaceAll("^0+|\\D+$", "");

                                break;
                            case NUMERIC:
                                if (DateUtil.isCellDateFormatted(cellC)) {
                                    Date dateValueC = cellC.getDateCellValue();

                                } else {
                                    double numericValueC = cellC.getNumericCellValue();
                                }
                                break;
                            default:
                                break;
                        }
                    }

                    CodePair codePair = new CodePair();
                    codePair.setCodeNew(codeNew);
                    codePair.setCodeOld(codeOld);

                    listCodePair.add(codePair);

                }
            }

            Iterator<Row> rowIterator = sheet2.rowIterator();

            int columnIndexSheet2 = -1;
            String valueToCompare;
            int rowIndex = 0;

            Row headerRow = rowIterator.next();
            Iterator<Cell> headerCellIterator = headerRow.cellIterator();
            while (headerCellIterator.hasNext()) {
                Cell headerCell = headerCellIterator.next();
                if (headerCell.getCellTypeEnum() == CellType.STRING) {
                    String headerCellValue = headerCell.getStringCellValue();
                    if (headerCellValue.equals("<<CODIGONUEVO>>")) {
                        columnIndexSheet2 = headerCell.getColumnIndex();
                        break;
                    }
                }
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                Cell cell = row.getCell(columnIndexSheet2);
                if (cell != null && cell.getCellTypeEnum() == CellType.STRING) {

                    String cellValue = cell.getStringCellValue();

                    int startIndex = cellValue.indexOf('-') + 1;
                    String substring = cellValue.substring(startIndex);
                    valueToCompare = substring.replaceAll("^0+|\\D+$", "");

                    for (CodePair codePair : listCodePair) {

                        if (codePair.getCodeNew() != null || codePair.getCodeOld() != null) {
                            if (valueToCompare != null) {
                                if ((codePair.getCodeNew() != null && codePair.getCodeNew().equals(valueToCompare))
                                        || (codePair.getCodeOld() != null && codePair.getCodeOld().equals(valueToCompare))) {

                                    Row newRow = newSheet.createRow(rowIndex);

                                    Iterator<Cell> cellIterator = row.cellIterator();
                                    int cellIndex = 0;
                                    while (cellIterator.hasNext()) {
                                        Cell originalCell = cellIterator.next();
                                        Cell newCell = newRow.createCell(cellIndex);

                                        CellType cellType = originalCell.getCellTypeEnum();
                                        if (null != cellType) {
                                            switch (cellType) {
                                                case STRING:
                                                    newCell.setCellValue(originalCell.getStringCellValue());
                                                    break;
                                                case NUMERIC:
                                                    newCell.setCellValue(originalCell.getNumericCellValue());
                                                    break;
                                                case BOOLEAN:
                                                    newCell.setCellValue(originalCell.getBooleanCellValue());
                                                    break;
                                                case BLANK:
                                                    break;
                                                default:
                                                    break;
                                            }
                                        }

                                        CellStyle originalCellStyle = originalCell.getCellStyle();
                                        CellStyle newCellStyle = newWorkbook.createCellStyle();
                                        newCellStyle.cloneStyleFrom(originalCellStyle);
                                        newCell.setCellStyle(newCellStyle);

                                        cellIndex++;
                                    }

                                    rowIndex++;

                                    System.err.println("response:" + valueToCompare);
                                }
                            }
                        }

                    }
                }
            }

        }

        try (FileOutputStream fos = new FileOutputStream(uploadDirectory + "response.xlsx")) {
            newWorkbook.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadFile(newWorkbook);

    }

    public void downloadFile(XSSFWorkbook workbook) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            workbook.close();

            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=response.xlsx");

            OutputStream outputStream = response.getOutputStream();
            baos.writeTo(outputStream);
            outputStream.flush();
            outputStream.close();

            facesContext.responseComplete();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    protected String realPath() {
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        return servletContext.getRealPath("");
    }

    protected void messageOfError(String message) {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, message, ""));
    }

    /**
     * @return the download
     */
    public StreamedContent getDownload() {
        return download;
    }

    /**
     * @param download the download to set
     */
    public void setDownload(StreamedContent download) {
        this.download = download;
    }

    /**
     * @return the selectedFileNameMain
     */
    public String getSelectedFileNameMain() {
        return selectedFileNameMain;
    }

    /**
     * @param selectedFileNameMain the selectedFileNameMain to set
     */
    public void setSelectedFileNameMain(String selectedFileNameMain) {
        this.selectedFileNameMain = selectedFileNameMain;
    }

    /**
     * @return the selectedFileNameCompare
     */
    public String getSelectedFileNameCompare() {
        return selectedFileNameCompare;
    }

    /**
     * @param selectedFileNameCompare the selectedFileNameCompare to set
     */
    public void setSelectedFileNameCompare(String selectedFileNameCompare) {
        this.selectedFileNameCompare = selectedFileNameCompare;
    }

    /**
     * @return the listCodePair
     */
    public List<CodePair> getListCodePair() {
        return listCodePair;
    }

    /**
     * @param listCodePair the listCodePair to set
     */
    public void setListCodePair(List<CodePair> listCodePair) {
        this.listCodePair = listCodePair;
    }

}
