/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.controllers;

import com.findkme.entities.Assets;
import com.findkme.entities.AssetsCompare;
import com.findkme.facades.AssetsCompareFacade;
import com.findkme.facades.AssetsFacade;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.bean.RequestScoped;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.event.FileUploadEvent;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRow;

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

    private String codeNew = null;
    private String codeOld = null;
    private String codeNewToCompare = null;

    private Map<CellStyle, CellStyle> styleMap = new HashMap<>();

    @EJB
    AssetsFacade assetsFacade;
    @EJB
    AssetsCompareFacade assetsCompareFacade;

    private String PATHNAME;

    private Assets newAsset = new Assets();
    private AssetsCompare newAssetCompare = new AssetsCompare();

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
        return originalFileName + timestamp + extension;
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

        try (FileInputStream file = new FileInputStream(uploadDirectory + selectedFileNameMain)) {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            System.err.println("Error:" + e);
        }

        return workbook;
    }

    public XSSFWorkbook readFileMainCompare() {
        XSSFWorkbook workbook = null;

        try (FileInputStream file = new FileInputStream(uploadDirectory + selectedFileNameCompare)) {
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

    public void saveData() {
        XSSFWorkbook workbookMain = readFileMain();
        XSSFSheet sheet = workbookMain.getSheetAt(0);

        int startRow = 4;
        int startColumn = 1;

        if (sheet != null) {
            try {
                for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);

                    String code_new = getStringCellValue(row, startColumn);
                    String code_old = getStringCellValue(row, startColumn + 1);
                    String type = getStringCellValue(row, startColumn + 2);
                    String brand = getStringCellValue(row, startColumn + 3);
                    String model = getStringCellValue(row, startColumn + 4);
                    String serial_number = getStringCellValue(row, startColumn + 5);
                    String description = getStringCellValue(row, startColumn + 6);
                    String state = getStringCellValue(row, startColumn + 7);
                    String location = getStringCellValue(row, startColumn + 8);
                    String custodian = getStringCellValue(row, startColumn + 9);
                    String campus = getStringCellValue(row, startColumn + 10);
                    String observations = getStringCellValue(row, startColumn + 11);
                    String hostname = getStringCellValue(row, startColumn + 12);
                    String career = getStringCellValue(row, startColumn + 13);

                    newAsset.setCode_new(code_new);
                    newAsset.setCode_old(code_old);
                    newAsset.setType(type);
                    newAsset.setBrand(brand);
                    newAsset.setModel(model);
                    newAsset.setSerial_number(serial_number);
                    newAsset.setDescription(description);
                    newAsset.setState(state);
                    newAsset.setLocation(location);
                    newAsset.setCustodian(custodian);
                    newAsset.setCampus(campus);
                    newAsset.setObservations(observations);
                    newAsset.setHostname(hostname);
                    newAsset.setCareer(career);

                    assetsFacade.create(newAsset);
                }
            } catch (Exception e) {
                System.err.println("error:" + e);
            }
        } else {
            System.err.println("workbookMain or sheet is null");
        }

    }

    public void fillValuesToOriginalFile() {
        XSSFWorkbook workbookMain = readFileMain();

        Font font = workbookMain.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());

//        Workbook wb = new XSSFWorkbook();
        XSSFCellStyle cellStyleQRed = workbookMain.createCellStyle();
        cellStyleQRed.setFillForegroundColor(IndexedColors.RED.getIndex());
        cellStyleQRed.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyleQRed.setAlignment(HorizontalAlignment.CENTER);
        cellStyleQRed.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyleQRed.setBorderTop(BorderStyle.THIN);
        cellStyleQRed.setBorderBottom(BorderStyle.THIN);
        cellStyleQRed.setBorderLeft(BorderStyle.THIN);
        cellStyleQRed.setBorderRight(BorderStyle.THIN);
        cellStyleQRed.setFont(font);

        XSSFCellStyle cellStyleQGreen = workbookMain.createCellStyle();
        cellStyleQGreen.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        cellStyleQGreen.setFont(font);
        cellStyleQGreen.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cellStyleQGreen.setAlignment(HorizontalAlignment.CENTER);
        cellStyleQGreen.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyleQGreen.setBorderTop(BorderStyle.THIN);
        cellStyleQGreen.setBorderBottom(BorderStyle.THIN);
        cellStyleQGreen.setBorderLeft(BorderStyle.THIN);
        cellStyleQGreen.setBorderRight(BorderStyle.THIN);

        XSSFCellStyle cellStyle = workbookMain.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);

        List<XSSFSheet> sheetList = new ArrayList<>();

        List<Assets> assetsThatNotContainG = assetsFacade.findAllAssets();

        for (int i = 0; i < workbookMain.getNumberOfSheets(); i++) {
            XSSFSheet sheet = (XSSFSheet) workbookMain.getSheetAt(i);
            sheetList.add(sheet);
        }

        int startRowNum = 11;
        for (int i = 0; i < sheetList.size(); i++) {

            XSSFSheet sheet = sheetList.get(i);

            if (sheet != null) {
                try {
                    switch (i) {
                        case 0: {
                            int lastRowNum = sheet.getLastRowNum();

                            if (startRowNum > lastRowNum) {
                                startRowNum = lastRowNum;
                            }
                            int startColNum = 2;
                            for (int rowNum = startRowNum; rowNum <= lastRowNum; rowNum++) {
                                XSSFRow row = sheet.getRow(rowNum);
                                if (row != null) {
                                    for (int colNum = startColNum; colNum < row.getLastCellNum(); colNum++) {
                                        XSSFCell cell = row.getCell(colNum);
                                        if (cell != null) {

                                            String cellValue = cell.getStringCellValue();

                                            if (colNum + 1 == 3) {

                                                String getAbbreviation;
                                                String getNumberRemovedZeroLeft;

                                                if (cellValue.contains("-")) {
                                                    getAbbreviation = cellValue.split("-")[0].trim();
                                                    getNumberRemovedZeroLeft = cellValue.split("-")[1].replaceAll("^0+", "");
                                                } else {
                                                    String valueOfCell = cellValue.trim();
                                                    getAbbreviation = valueOfCell.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeft = valueOfCell.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeft = getNumberRemovedZeroLeft.replaceAll("^0+", "");
                                                }

                                                String getFirstAndLastLetter = "";

                                                if (getAbbreviation.length() > 1) {
                                                    getFirstAndLastLetter = getAbbreviation.substring(0, 1) + getAbbreviation.substring(getAbbreviation.length() - 1);
                                                } else if (getAbbreviation.length() == 1) {
                                                    getFirstAndLastLetter = getAbbreviation + getAbbreviation;
                                                }

                                                for (Assets assets : assetsThatNotContainG) {

                                                    String getAbbreviationOfAssets;
                                                    String getNumberRemovedZeroLeftAssets;

                                                    String getAbbreviationOfAssetsOldCode;
                                                    String getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (assets.getCode_new().contains("-")) {

                                                        getAbbreviationOfAssets = assets.getCode_new().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfCell = assets.getCode_new().trim();
                                                        getAbbreviationOfAssets = valueOfCell.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssets = getNumberRemovedZeroLeftAssets.replaceAll("^0+", "");
                                                    }

                                                    if (assets.getCode_old().contains("-")) {

                                                        getAbbreviationOfAssetsOldCode = assets.getCode_old().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfAsset = assets.getCode_old().trim();
                                                        getAbbreviationOfAssetsOldCode = valueOfAsset.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = getNumberRemovedZeroLeftAssetsOldCode.replaceAll("^0+", "");
                                                    }

                                                    String getFirstAndLastLetterAssets = "";
                                                    String getFirstAndLastLetterAssetsOld = "";

                                                    if (getAbbreviationOfAssets.length() > 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets.substring(0, 1) + getAbbreviationOfAssets.substring(getAbbreviationOfAssets.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets + getAbbreviationOfAssets;
                                                    }

                                                    if (getAbbreviationOfAssetsOldCode.length() > 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode.substring(0, 1) + getAbbreviationOfAssetsOldCode.substring(getAbbreviationOfAssetsOldCode.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode + getAbbreviationOfAssetsOldCode;
                                                    }

                                                    String joinToCompareFirst = getFirstAndLastLetter + getNumberRemovedZeroLeft;
                                                    String joinToCompareMainCodeNew = getFirstAndLastLetterAssets + getNumberRemovedZeroLeftAssets;
                                                    String joinToCompareMainCodeOld = getFirstAndLastLetterAssetsOld + getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (joinToCompareFirst.length() > 0 && joinToCompareMainCodeNew.length() > 0 || joinToCompareMainCodeOld.length() > 0) {

                                                        if (joinToCompareFirst.isEmpty()) {
                                                            joinToCompareFirst = "Empty1";
                                                        }

                                                        if (joinToCompareMainCodeNew.isEmpty()) {
                                                            joinToCompareMainCodeNew = "Empty3";
                                                        }

                                                        if (joinToCompareMainCodeOld.isEmpty()) {
                                                            joinToCompareMainCodeOld = "Empty4";
                                                        }

                                                        if (joinToCompareFirst.equals(joinToCompareMainCodeNew) || joinToCompareFirst.equals(joinToCompareMainCodeOld)) {

                                                            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

                                                            XSSFCell cellD = row.getCell(colNum + 1);
                                                            XSSFCell cellE = row.getCell(colNum + 2);

                                                            // Asset is used
                                                            XSSFCell cellI = row.getCell(colNum + 6);
                                                            XSSFCell cellJ = row.getCell(colNum + 7);

                                                            XSSFCell cellL = row.getCell(colNum + 9);

                                                            // state asset
                                                            XSSFCell cellN = row.getCell(colNum + 11);
                                                            XSSFCell cellO = row.getCell(colNum + 12);
                                                            XSSFCell cellP = row.getCell(colNum + 13);

                                                            // observations
                                                            XSSFCell cellR = row.getCell(colNum + 15);

                                                            String cellValueD = cellD.getStringCellValue();
                                                            String cellValueE = cellE.getStringCellValue();

                                                            int distanceA = levenshteinDistance.apply(cellValueD, assets.getType());
                                                            int distanceB = levenshteinDistance.apply(cellValueE, assets.getBrand());

                                                            int distanceC = levenshteinDistance.apply(cellValueD, assets.getDescription());
                                                            int distanceD = levenshteinDistance.apply(cellValueE, assets.getDescription());

                                                            int maxLengthA = Math.max(cellValueD.length(), assets.getType().length());
                                                            double similarityPercentageA = (1 - (double) distanceA / maxLengthA) * 100;

                                                            int maxLengthB = Math.max(cellValueE.length(), assets.getBrand().length());
                                                            double similarityPercentageB = (1 - (double) distanceB / maxLengthB) * 100;

                                                            int maxLengthC = Math.max(cellValueD.length(), assets.getDescription().length());
                                                            double similarityPercentageC = (1 - (double) distanceC / maxLengthC) * 100;

                                                            int maxLengthD = Math.max(cellValueE.length(), assets.getDescription().length());
                                                            double similarityPercentageD = (1 - (double) distanceD / maxLengthD) * 100;

                                                            int plusSimilarity = (int) Math.round(similarityPercentageA + similarityPercentageB + similarityPercentageC + similarityPercentageD);

                                                            if (plusSimilarity >= 7) {
//                                                                System.err.println("Porcentaje de coincidencia");
                                                            } else {
//                                                                System.err.println("NO Porcentaje de coincidencia");
                                                            }

                                                            if (assets.getState().contains("MAL") || assets.getObservations().contains("MAL") || assets.getObservations().contains("VIEJ") || assets.getObservations().contains("INUTIL") || assets.getObservations().contains("DESHUSO") || assets.getObservations().contains("DESUSO") || assets.getObservations().contains("NO SIRVE") || assets.getObservations().contains("DESGASTADO") || assets.getObservations().contains("SIN USO") || assets.getObservations().isEmpty() || assets.getLocation().contains("PATIO")) {
                                                                cellJ.setCellValue("NO");
                                                                cellJ.setCellStyle(cellStyle);
                                                            } else {
                                                                cellI.setCellValue("SI");
                                                                cellI.setCellStyle(cellStyle);
                                                            }

                                                            if (assets.getState().contains("MAL")) {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("REGULAR")) {
                                                                cellO.setCellValue("R");
                                                                cellO.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BUEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BIEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            }

//                                                            System.err.println("Code: " + assets.getCode_new());
//                                                            System.err.println("Code Old: " + assets.getCode_old());
                                                            cellL.setCellValue("NO");
                                                            cellL.setCellStyle(cellStyle);

                                                            cellR.setCellValue(assets.getObservations());
                                                            cellR.setCellStyle(cellStyle);

                                                            XSSFCell cellQ = row.getCell(colNum + 14);
                                                            if (cellQ == null) {
                                                                cellQ = row.createCell(colNum + 14);
                                                            }
                                                            cellQ.setCellValue("ENCONTRADO");
                                                            cellQ.setCellStyle(cellStyleQGreen);

                                                            String IsUsed = cellI.getStringCellValue();
                                                            String NotUsed = cellJ.getStringCellValue();

                                                            XSSFCell cellH = row.getCell(colNum + 5);

                                                            if (!IsUsed.isEmpty() && !NotUsed.isEmpty()) {
                                                                cellH.setCellValue("SI");
                                                                cellH.setCellStyle(cellStyle);
                                                            } else {
                                                                cellH.setCellValue("NO");
                                                                cellH.setCellStyle(cellStyle);
                                                            }

                                                        } else {
                                                            XSSFCell cellQ = row.getCell(colNum + 14);

                                                            if (cellQ.getStringCellValue().length() == 0) {
                                                                cellQ.setCellValue("NO ENCONTRADO");
                                                                cellQ.setCellStyle(cellStyleQRed);

                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case 1: {
                            int lastRowNum = sheet.getLastRowNum();
                            if (startRowNum > lastRowNum) {
                                startRowNum = lastRowNum;
                            }
                            int startColNum = 2;
                            for (int rowNum = startRowNum; rowNum <= lastRowNum; rowNum++) {
                                XSSFRow row = sheet.getRow(rowNum);
                                if (row != null) {
                                    for (int colNum = startColNum; colNum < row.getLastCellNum(); colNum++) {
                                        XSSFCell cell = row.getCell(colNum);
                                        if (cell != null) {

                                            String cellValue = cell.getStringCellValue();

                                            if (colNum + 1 == 3) {

                                                String getAbbreviation;
                                                String getNumberRemovedZeroLeft;

                                                if (cellValue.contains("-")) {
                                                    getAbbreviation = cellValue.split("-")[0].trim();
                                                    getNumberRemovedZeroLeft = cellValue.split("-")[1].replaceAll("^0+", "");
                                                } else {
                                                    String valueOfCell = cellValue.trim();
                                                    getAbbreviation = valueOfCell.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeft = valueOfCell.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeft = getNumberRemovedZeroLeft.replaceAll("^0+", "");
                                                }

                                                String getFirstAndLastLetter = "";

                                                if (getAbbreviation.length() > 1) {
                                                    getFirstAndLastLetter = getAbbreviation.substring(0, 1) + getAbbreviation.substring(getAbbreviation.length() - 1);
                                                } else if (getAbbreviation.length() == 1) {
                                                    getFirstAndLastLetter = getAbbreviation + getAbbreviation;
                                                }

                                                for (Assets assets : assetsThatNotContainG) {

                                                    String getAbbreviationOfAssets;
                                                    String getNumberRemovedZeroLeftAssets;

                                                    String getAbbreviationOfAssetsOldCode;
                                                    String getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (assets.getCode_new().contains("-")) {

                                                        getAbbreviationOfAssets = assets.getCode_new().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfCell = assets.getCode_new().trim();
                                                        getAbbreviationOfAssets = valueOfCell.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssets = getNumberRemovedZeroLeftAssets.replaceAll("^0+", "");
                                                    }

                                                    if (assets.getCode_old().contains("-")) {

                                                        getAbbreviationOfAssetsOldCode = assets.getCode_old().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfAsset = assets.getCode_old().trim();
                                                        getAbbreviationOfAssetsOldCode = valueOfAsset.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = getNumberRemovedZeroLeftAssetsOldCode.replaceAll("^0+", "");
                                                    }

                                                    String getFirstAndLastLetterAssets = null;
                                                    String getFirstAndLastLetterAssetsOld = null;

                                                    if (getAbbreviationOfAssets.length() > 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets.substring(0, 1) + getAbbreviationOfAssets.substring(getAbbreviationOfAssets.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets + getAbbreviationOfAssets;
                                                    }

                                                    if (getAbbreviationOfAssetsOldCode.length() > 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode.substring(0, 1) + getAbbreviationOfAssetsOldCode.substring(getAbbreviationOfAssetsOldCode.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode + getAbbreviationOfAssetsOldCode;
                                                    }

                                                    String joinToCompareFirst = getFirstAndLastLetter + getNumberRemovedZeroLeft;
                                                    String joinToCompareMainCodeNew = getFirstAndLastLetterAssets + getNumberRemovedZeroLeftAssets;
                                                    String joinToCompareMainCodeOld = getFirstAndLastLetterAssetsOld + getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (joinToCompareFirst.length() > 0 && joinToCompareMainCodeNew.length() > 0 || joinToCompareMainCodeOld.length() > 0) {

                                                        if (joinToCompareFirst.isEmpty()) {
                                                            joinToCompareFirst = "Empty1";
                                                        }

                                                        if (joinToCompareMainCodeNew.isEmpty()) {
                                                            joinToCompareMainCodeNew = "Empty3";
                                                        }

                                                        if (joinToCompareMainCodeOld.isEmpty()) {
                                                            joinToCompareMainCodeOld = "Empty4";
                                                        }

                                                        if (joinToCompareFirst.equals(joinToCompareMainCodeNew) || joinToCompareFirst.equals(joinToCompareMainCodeOld)) {

                                                            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

                                                            XSSFCell cellD = row.getCell(colNum + 1);
                                                            XSSFCell cellE = row.getCell(colNum + 2);

                                                            // Asset is used
                                                            XSSFCell cellI = row.getCell(colNum + 6);
                                                            XSSFCell cellJ = row.getCell(colNum + 7);

                                                            XSSFCell cellL = row.getCell(colNum + 9);

                                                            // state asset
                                                            XSSFCell cellN = row.getCell(colNum + 11);
                                                            XSSFCell cellO = row.getCell(colNum + 12);
                                                            XSSFCell cellP = row.getCell(colNum + 13);

                                                            // observations
                                                            XSSFCell cellR = row.getCell(colNum + 15);

                                                            String cellValueD = cellD.getStringCellValue();
                                                            String cellValueE = cellE.getStringCellValue();

                                                            int distanceA = levenshteinDistance.apply(cellValueD, assets.getType());
                                                            int distanceB = levenshteinDistance.apply(cellValueE, assets.getBrand());

                                                            int distanceC = levenshteinDistance.apply(cellValueD, assets.getDescription());
                                                            int distanceD = levenshteinDistance.apply(cellValueE, assets.getDescription());

                                                            int maxLengthA = Math.max(cellValueD.length(), assets.getType().length());
                                                            double similarityPercentageA = (1 - (double) distanceA / maxLengthA) * 100;

                                                            int maxLengthB = Math.max(cellValueE.length(), assets.getBrand().length());
                                                            double similarityPercentageB = (1 - (double) distanceB / maxLengthB) * 100;

                                                            int maxLengthC = Math.max(cellValueD.length(), assets.getDescription().length());
                                                            double similarityPercentageC = (1 - (double) distanceC / maxLengthC) * 100;

                                                            int maxLengthD = Math.max(cellValueE.length(), assets.getDescription().length());
                                                            double similarityPercentageD = (1 - (double) distanceD / maxLengthD) * 100;

                                                            int plusSimilarity = (int) Math.round(similarityPercentageA + similarityPercentageB + similarityPercentageC + similarityPercentageD);

                                                            if (plusSimilarity >= 7) {
//                                                                System.err.println("Porcentaje de coincidencia");
                                                            } else {
//                                                                System.err.println("NO Porcentaje de coincidencia");
                                                            }

                                                            if (assets.getState().contains("MAL") || assets.getObservations().contains("MAL") || assets.getObservations().contains("VIEJ") || assets.getObservations().contains("INUTIL") || assets.getObservations().contains("DESHUSO") || assets.getObservations().contains("DESUSO") || assets.getObservations().contains("NO SIRVE") || assets.getObservations().contains("DESGASTADO") || assets.getObservations().contains("SIN USO") || assets.getObservations().isEmpty() || assets.getLocation().contains("PATIO")) {
                                                                cellJ.setCellValue("NO");
                                                                cellJ.setCellStyle(cellStyle);
                                                            } else {
                                                                cellI.setCellValue("SI");
                                                                cellI.setCellStyle(cellStyle);
                                                            }

                                                            if (assets.getState().contains("MAL")) {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("REGULAR")) {
                                                                cellO.setCellValue("R");
                                                                cellO.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BUEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BIEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            }

                                                            cellL.setCellValue("NO");
                                                            cellL.setCellStyle(cellStyle);

                                                            cellR.setCellValue(assets.getObservations());
                                                            cellR.setCellStyle(cellStyle);

                                                            XSSFCell cellQ = row.getCell(colNum + 14);
                                                            if (cellQ == null) {
                                                                cellQ = row.createCell(colNum + 14);
                                                            }
                                                            cellQ.setCellValue("ENCONTRADO");
                                                            cellQ.setCellStyle(cellStyleQGreen);

                                                            String IsUsed = cellI.getStringCellValue();
                                                            String NotUsed = cellJ.getStringCellValue();

                                                            XSSFCell cellH = row.getCell(colNum + 5);

                                                            if (!IsUsed.isEmpty() && !NotUsed.isEmpty()) {
                                                                cellH.setCellValue("SI");
                                                                cellH.setCellStyle(cellStyle);
                                                            } else {
                                                                cellH.setCellValue("NO");
                                                                cellH.setCellStyle(cellStyle);
                                                            }

                                                        } else {
                                                            XSSFCell cellQ = row.getCell(colNum + 14);

                                                            if (cellQ.getStringCellValue().length() == 0) {
                                                                cellQ.setCellValue("NO ENCONTRADO");
                                                                cellQ.setCellStyle(cellStyleQRed);

                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case 2: {
                            int lastRowNum = sheet.getLastRowNum();
                            if (startRowNum > lastRowNum) {
                                startRowNum = lastRowNum;
                            }
                            int startColNum = 2;
                            for (int rowNum = startRowNum; rowNum <= lastRowNum; rowNum++) {
                                XSSFRow row = sheet.getRow(rowNum);

                                if (row != null) {
                                    for (int colNum = startColNum; colNum < row.getLastCellNum(); colNum++) {
                                        XSSFCell cell = row.getCell(colNum);
                                        if (cell != null) {

//                                            String cellValue = cell.getStringCellValue();
                                            String cellValue;
                                            if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                                                cellValue = String.valueOf(Math.round(Math.round(cell.getNumericCellValue())));
                                            } else {
                                                cellValue = cell.getStringCellValue();
                                            }

                                            if (colNum + 1 == 3) {

                                                String getAbbreviation = "";
                                                String getNumberRemovedZeroLeft = "";

                                                if (cellValue.contains("-")) {
                                                    if (!cellValue.equals("-")) {
                                                        if (cellValue.length() > 1) {
                                                            getAbbreviation = cellValue.split("-")[0].trim();
                                                            getNumberRemovedZeroLeft = cellValue.split("-")[1].replaceAll("^0+", "");
                                                        }
                                                    }

                                                } else {
                                                    String valueOfCell = cellValue.trim();
                                                    getAbbreviation = valueOfCell.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeft = valueOfCell.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeft = getNumberRemovedZeroLeft.replaceAll("^0+", "");
                                                }
                                                String getFirstAndLastLetter = "";

                                                if (getAbbreviation.length() > 1) {
                                                    getFirstAndLastLetter = getAbbreviation.substring(0, 1) + getAbbreviation.substring(getAbbreviation.length() - 1);
                                                } else if (getAbbreviation.length() == 1) {
                                                    getFirstAndLastLetter = getAbbreviation + getAbbreviation;
                                                }

                                                XSSFCell cellE = row.getCell(colNum + 2);
                                                String cellValueCodeOldE;
                                                if (cellE.getCellTypeEnum() == CellType.NUMERIC) {
                                                    cellValueCodeOldE = String.valueOf(Math.round(cellE.getNumericCellValue()));
                                                } else {
                                                    cellValueCodeOldE = cellE.getStringCellValue();
                                                }
                                                String cellValueOldCodeE = cellValueCodeOldE;

                                                String getAbbreviationOldCodeE = "";
                                                String getNumberRemovedZeroLeftOldCodeE = "";

                                                if (cellValueOldCodeE.contains("-")) {
                                                    if (!cellValueOldCodeE.equals("-")) {
                                                        if (cellValueOldCodeE.length() > 1) {
                                                            getAbbreviationOldCodeE = cellValueOldCodeE.split("-")[0].trim();
                                                            getNumberRemovedZeroLeftOldCodeE = cellValueOldCodeE.split("-")[1].replaceAll("^0+", "");
                                                        }
                                                    }

                                                } else {
                                                    String valueOfCellOfOldCodeE = cellValueOldCodeE.trim();
                                                    getAbbreviationOldCodeE = valueOfCellOfOldCodeE.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeftOldCodeE = valueOfCellOfOldCodeE.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeftOldCodeE = getNumberRemovedZeroLeftOldCodeE.replaceAll("^0+", "");
                                                }

                                                String getFirstAndLastLetterOfOldCode = "";

                                                if (getAbbreviationOldCodeE.length() > 1) {
                                                    getFirstAndLastLetterOfOldCode = getAbbreviationOldCodeE.substring(0, 1) + getAbbreviationOldCodeE.substring(getAbbreviationOldCodeE.length() - 1);
                                                } else if (getAbbreviationOldCodeE.length() == 1) {
                                                    getFirstAndLastLetterOfOldCode = getAbbreviationOldCodeE + getAbbreviationOldCodeE;
                                                }

                                                for (Assets assets : assetsThatNotContainG) {

                                                    String getAbbreviationOfAssets = "";
                                                    String getNumberRemovedZeroLeftAssets = "";

                                                    String getAbbreviationOfAssetsOldCode = "";
                                                    String getNumberRemovedZeroLeftAssetsOldCode = "";

                                                    if (assets.getCode_new().contains("-")) {
                                                        if (!assets.getCode_new().equals("-")) {
                                                            getAbbreviationOfAssets = assets.getCode_new().split("-")[0].trim();
                                                            getNumberRemovedZeroLeftAssets = assets.getCode_new().split("-")[1].replaceAll("^0+", "");
                                                        }

                                                    } else {
                                                        String valueOfCell = assets.getCode_new().trim();
                                                        getAbbreviationOfAssets = valueOfCell.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssets = getNumberRemovedZeroLeftAssets.replaceAll("^0+", "");
                                                    }

                                                    if (assets.getCode_old().contains("-")) {
                                                        if (!assets.getCode_old().equals("-")) {
                                                            getAbbreviationOfAssetsOldCode = assets.getCode_old().split("-")[0].trim();
                                                            getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().split("-")[1].replaceAll("^0+", "");
                                                        }

                                                    } else {
                                                        String valueOfAsset = assets.getCode_old().trim();
                                                        getAbbreviationOfAssetsOldCode = valueOfAsset.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = getNumberRemovedZeroLeftAssetsOldCode.replaceAll("^0+", "");
                                                    }

                                                    String getFirstAndLastLetterAssets = "";
                                                    String getFirstAndLastLetterAssetsOld = "";

                                                    if (getAbbreviationOfAssets.length() > 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets.substring(0, 1) + getAbbreviationOfAssets.substring(getAbbreviationOfAssets.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets + getAbbreviationOfAssets;
                                                    }

                                                    if (getAbbreviationOfAssetsOldCode.length() > 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode.substring(0, 1) + getAbbreviationOfAssetsOldCode.substring(getAbbreviationOfAssetsOldCode.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode + getAbbreviationOfAssetsOldCode;
                                                    }

                                                    String joinToCompareFirst = getFirstAndLastLetter + getNumberRemovedZeroLeft;
                                                    String joinToCompareFirstCodeOld = getFirstAndLastLetterOfOldCode + getNumberRemovedZeroLeftOldCodeE;
                                                    String joinToCompareMainCodeNew = getFirstAndLastLetterAssets + getNumberRemovedZeroLeftAssets;
                                                    String joinToCompareMainCodeOld = getFirstAndLastLetterAssetsOld + getNumberRemovedZeroLeftAssetsOldCode;

                                                    if ((joinToCompareFirst.length() > 0 || joinToCompareFirstCodeOld.length() > 0) && joinToCompareMainCodeNew.length() > 0 || joinToCompareMainCodeOld.length() > 0) {

                                                        if (joinToCompareFirst.isEmpty()) {
                                                            joinToCompareFirst = "Empty1";
                                                        }

                                                        if (joinToCompareFirstCodeOld.isEmpty()) {
                                                            joinToCompareFirstCodeOld = "Empty2";
                                                        }

                                                        if (joinToCompareMainCodeNew.isEmpty()) {
                                                            joinToCompareMainCodeNew = "Empty3";
                                                        }

                                                        if (joinToCompareMainCodeOld.isEmpty()) {
                                                            joinToCompareMainCodeOld = "Empty4";
                                                        }

                                                        if (joinToCompareFirst.equals(joinToCompareMainCodeNew) || joinToCompareFirst.equals(joinToCompareMainCodeOld) || joinToCompareFirstCodeOld.equals(joinToCompareMainCodeNew) || joinToCompareFirstCodeOld.equals(joinToCompareMainCodeOld)) {

                                                            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

                                                            XSSFCell cellD = row.getCell(colNum + 1);
// Asset is used
                                                            XSSFCell cellJ = row.getCell(colNum + 7);
                                                            XSSFCell cellK = row.getCell(colNum + 8);

                                                            XSSFCell cellM = row.getCell(colNum + 10);
// state asset
                                                            XSSFCell cellO = row.getCell(colNum + 12);
                                                            XSSFCell cellP = row.getCell(colNum + 13);
                                                            XSSFCell cellQ = row.getCell(colNum + 14);
//// observations
                                                            XSSFCell cellS = row.getCell(colNum + 16);

                                                            String cellValueD = cellD.getStringCellValue();

                                                            int distanceA = levenshteinDistance.apply(cellValueD, assets.getType());
                                                            int distanceB = levenshteinDistance.apply(cellValueD, assets.getBrand());

                                                            int maxLengthA = Math.max(cellValueD.length(), assets.getType().length());
                                                            double similarityPercentageA = (1 - (double) distanceA / maxLengthA) * 100;

                                                            int maxLengthB = Math.max(cellValueD.length(), assets.getBrand().length());
                                                            double similarityPercentageB = (1 - (double) distanceB / maxLengthB) * 100;

                                                            int plusSimilarity = (int) Math.round(similarityPercentageA + similarityPercentageB);

                                                            if (plusSimilarity >= 7) {
                                                                System.err.println("Porcentaje de coincidencia");
                                                            } else {
                                                                System.err.println("NO Porcentaje de coincidencia");
                                                            }

                                                            if (assets.getState().contains("MAL") || assets.getObservations().contains("MAL") || assets.getObservations().contains("VIEJ") || assets.getObservations().contains("INUTIL") || assets.getObservations().contains("DESHUSO") || assets.getObservations().contains("DESUSO") || assets.getObservations().contains("NO SIRVE") || assets.getObservations().contains("DESGASTADO") || assets.getObservations().contains("SIN USO") || assets.getObservations().isEmpty() || assets.getLocation().contains("PATIO")) {
                                                                cellK.setCellValue("NO");
                                                                cellK.setCellStyle(cellStyle);
                                                            } else {
                                                                cellJ.setCellValue("SI");
                                                                cellJ.setCellStyle(cellStyle);
                                                            }

                                                            if (assets.getState().contains("MAL")) {
                                                                cellQ.setCellValue("M");
                                                                cellQ.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("REGULAR")) {
                                                                cellP.setCellValue("R");
                                                                cellP.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BUEN")) {
                                                                cellO.setCellValue("B");
                                                                cellO.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BIEN")) {
                                                                cellO.setCellValue("B");
                                                                cellO.setCellStyle(cellStyle);
                                                            } else {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            }

                                                            cellM.setCellValue("NO");
                                                            cellM.setCellStyle(cellStyle);

                                                            cellS.setCellValue(assets.getObservations());
                                                            cellS.setCellStyle(cellStyle);

                                                            XSSFCell cellR = row.getCell(colNum + 15);
                                                            if (cellR == null) {
                                                                cellR = row.createCell(colNum + 15);
                                                            }
                                                            cellR.setCellValue("ENCONTRADO");
                                                            cellR.setCellStyle(cellStyleQGreen);

                                                            String IsUsed = cellK.getStringCellValue();
                                                            String NotUsed = cellJ.getStringCellValue();

                                                            XSSFCell cellI = row.getCell(colNum + 6);

                                                            if (!IsUsed.isEmpty() && !NotUsed.isEmpty()) {
                                                                cellI.setCellValue("SI");
                                                                cellI.setCellStyle(cellStyle);
                                                            } else {
                                                                cellI.setCellValue("NO");
                                                                cellI.setCellStyle(cellStyle);
                                                            }
                                                        } else {
                                                            XSSFCell cellR = row.getCell(colNum + 15);

                                                            if (cellR.getStringCellValue().length() == 0) {
                                                                cellR.setCellValue("NO ENCONTRADO");
                                                                cellR.setCellStyle(cellStyleQRed);
                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case 3: {
                            int lastRowNum = sheet.getLastRowNum();
                            if (startRowNum > lastRowNum) {
                                startRowNum = lastRowNum;
                            }
                            int startColNum = 3;
                            for (int rowNum = startRowNum; rowNum <= lastRowNum; rowNum++) {
                                XSSFRow row = sheet.getRow(rowNum);

                                if (row != null) {
                                    for (int colNum = startColNum; colNum < row.getLastCellNum(); colNum++) {
                                        XSSFCell cell = row.getCell(colNum);
                                        if (cell != null) {

                                            String cellValue;
                                            if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                                                cellValue = String.valueOf(Math.round(cell.getNumericCellValue()));
                                            } else {
                                                cellValue = cell.getStringCellValue();
                                            }

                                            if (colNum + 1 == 4) {

                                                String getAbbreviation = "";
                                                String getNumberRemovedZeroLeft = "";

                                                if (cellValue.contains("-")) {
                                                    if (cellValue.length() > 1) {
                                                        getAbbreviation = cellValue.split("-")[0].trim();
                                                        getNumberRemovedZeroLeft = cellValue.split("-")[1].replaceAll("^0+", "");
                                                    }
                                                } else {
                                                    String valueOfCell = cellValue.trim();
                                                    getAbbreviation = valueOfCell.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeft = valueOfCell.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeft = getNumberRemovedZeroLeft.replaceAll("^0+", "");
                                                }
                                                String getFirstAndLastLetter = "";

                                                if (getAbbreviation.length() > 1) {
                                                    getFirstAndLastLetter = getAbbreviation.substring(0, 1) + getAbbreviation.substring(getAbbreviation.length() - 1);
                                                } else if (getAbbreviation.length() == 1) {
                                                    getFirstAndLastLetter = getAbbreviation + getAbbreviation;
                                                }

                                                for (Assets assets : assetsThatNotContainG) {

                                                    String getAbbreviationOfAssets;
                                                    String getNumberRemovedZeroLeftAssets;

                                                    String getAbbreviationOfAssetsOldCode;
                                                    String getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (assets.getCode_new().contains("-")) {

                                                        getAbbreviationOfAssets = assets.getCode_new().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfCell = assets.getCode_new().trim();
                                                        getAbbreviationOfAssets = valueOfCell.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssets = getNumberRemovedZeroLeftAssets.replaceAll("^0+", "");
                                                    }

                                                    if (assets.getCode_old().contains("-")) {

                                                        getAbbreviationOfAssetsOldCode = assets.getCode_old().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfAsset = assets.getCode_old().trim();
                                                        getAbbreviationOfAssetsOldCode = valueOfAsset.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = getNumberRemovedZeroLeftAssetsOldCode.replaceAll("^0+", "");
                                                    }

                                                    String getFirstAndLastLetterAssets = "";
                                                    String getFirstAndLastLetterAssetsOld = "";

                                                    if (getAbbreviationOfAssets.length() > 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets.substring(0, 1) + getAbbreviationOfAssets.substring(getAbbreviationOfAssets.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets + getAbbreviationOfAssets;
                                                    }

                                                    if (getAbbreviationOfAssetsOldCode.length() > 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode.substring(0, 1) + getAbbreviationOfAssetsOldCode.substring(getAbbreviationOfAssetsOldCode.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode + getAbbreviationOfAssetsOldCode;
                                                    }

                                                    String joinToCompareFirst = getFirstAndLastLetter + getNumberRemovedZeroLeft;
                                                    String joinToCompareMainCodeNew = getFirstAndLastLetterAssets + getNumberRemovedZeroLeftAssets;
                                                    String joinToCompareMainCodeOld = getFirstAndLastLetterAssetsOld + getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (joinToCompareFirst.length() > 0 && joinToCompareMainCodeNew.length() > 0 || joinToCompareMainCodeOld.length() > 0) {

//                                                        System.err.println("joinToCompareMainCodeOld: " + joinToCompareMainCodeOld);
//                                                        System.err.println("joinToCompareMainCodeOld: " + joinToCompareMainCodeOld);
                                                        if (joinToCompareFirst.isEmpty()) {
                                                            joinToCompareFirst = "Empty1";
                                                        }

                                                        if (joinToCompareMainCodeNew.isEmpty()) {
                                                            joinToCompareMainCodeNew = "Empty3";
                                                        }

                                                        if (joinToCompareMainCodeOld.isEmpty()) {
                                                            joinToCompareMainCodeOld = "Empty4";
                                                        }

                                                        if (joinToCompareFirst.equals(joinToCompareMainCodeNew) || joinToCompareFirst.equals(joinToCompareMainCodeOld)) {

                                                            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

                                                            XSSFCell cellE = row.getCell(colNum + 2);
                                                            XSSFCell cellF = row.getCell(colNum + 3);
//// Asset is used
                                                            XSSFCell cellL = row.getCell(colNum + 8);
                                                            XSSFCell cellM = row.getCell(colNum + 9);

                                                            XSSFCell cellO = row.getCell(colNum + 11);
//
////
////// state asset
                                                            XSSFCell cellQ = row.getCell(colNum + 13);
                                                            XSSFCell cellR = row.getCell(colNum + 14);

                                                            XSSFCell cellS = row.getCell(colNum + 15);
////// observations
                                                            XSSFCell cellU = row.getCell(colNum + 17);
////
                                                            String cellValueE = cellE.getStringCellValue();
                                                            String cellValueF = cellF.getStringCellValue();
//
                                                            int distanceA = levenshteinDistance.apply(cellValueE, assets.getType());
                                                            int distanceB = levenshteinDistance.apply(cellValueF, assets.getBrand());

                                                            int maxLengthA = Math.max(cellValueE.length(), assets.getType().length());
                                                            double similarityPercentageA = (1 - (double) distanceA / maxLengthA) * 100;

                                                            int maxLengthB = Math.max(cellValueF.length(), assets.getBrand().length());
                                                            double similarityPercentageB = (1 - (double) distanceB / maxLengthB) * 100;

                                                            int plusSimilarity = (int) Math.round(similarityPercentageA + similarityPercentageB);
//
                                                            if (plusSimilarity >= 7) {
//                                                                System.err.println("Porcentaje de coincidencia");
                                                            } else {
//                                                                System.err.println("NO Porcentaje de coincidencia");
                                                            }

                                                            System.err.println("assets: " + joinToCompareMainCodeNew);
                                                            System.err.println("assets 2: " + joinToCompareMainCodeOld);
                                                            if (assets.getState().contains("MAL") || assets.getObservations().contains("MAL") || assets.getObservations().contains("VIEJ") || assets.getObservations().contains("INUTIL") || assets.getObservations().contains("DESHUSO") || assets.getObservations().contains("DESUSO") || assets.getObservations().contains("NO SIRVE") || assets.getObservations().contains("DESGASTADO") || assets.getObservations().contains("SIN USO") || assets.getObservations().isEmpty() || assets.getLocation().contains("PATIO")) {
                                                                cellM.setCellValue("NO");
                                                                cellM.setCellStyle(cellStyle);
                                                            } else {
                                                                cellL.setCellValue("SI");
                                                                cellL.setCellStyle(cellStyle);
                                                            }

                                                            if (assets.getState().contains("MAL")) {
                                                                cellS.setCellValue("M");
                                                                cellS.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("REGULAR")) {
                                                                cellR.setCellValue("R");
                                                                cellR.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BUEN")) {
                                                                cellQ.setCellValue("B");
                                                                cellQ.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BIEN")) {
                                                                cellQ.setCellValue("B");
                                                                cellQ.setCellStyle(cellStyle);
                                                            } else {
                                                                cellS.setCellValue("M");
                                                                cellS.setCellStyle(cellStyle);
                                                            }

                                                            cellO.setCellValue("NO");
                                                            cellO.setCellStyle(cellStyle);

                                                            cellU.setCellValue(assets.getObservations());
                                                            cellU.setCellStyle(cellStyle);

                                                            XSSFCell cellT = row.getCell(colNum + 16);
                                                            if (cellT == null) {
                                                                cellT = row.createCell(colNum + 16);
                                                            }
                                                            cellT.setCellValue("ENCONTRADO");
                                                            cellT.setCellStyle(cellStyleQGreen);

                                                            XSSFCell cellK = row.getCell(colNum + 7);

                                                            String IsUsed = cellL.getStringCellValue();
                                                            String NotUsed = cellM.getStringCellValue();

                                                            if (!IsUsed.isEmpty() && !NotUsed.isEmpty()) {
                                                                cellK.setCellValue("SI");
                                                                cellK.setCellStyle(cellStyle);
                                                            } else {
                                                                cellK.setCellValue("NO");
                                                                cellK.setCellStyle(cellStyle);
                                                            }
                                                        } else {
                                                            XSSFCell cellT = row.getCell(colNum + 16);
                                                            if (cellT != null) {
                                                                if (cellT.getStringCellValue().length() == 0) {
                                                                    cellT.setCellValue("NO ENCONTRADO");
                                                                    cellT.setCellStyle(cellStyleQRed);
                                                                }
                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }
                        case 4: {

                            int lastRowNum = sheet.getLastRowNum();
                            int startRowNum4 = 10;

                            if (startRowNum4 > lastRowNum) {
                                startRowNum4 = lastRowNum;
                            }
                            int startColNum = 2;
                            for (int rowNum = startRowNum4; rowNum <= lastRowNum; rowNum++) {
                                XSSFRow row = sheet.getRow(rowNum);

                                if (row != null) {
                                    for (int colNum = startColNum; colNum < row.getLastCellNum(); colNum++) {
                                        XSSFCell cell = row.getCell(colNum);
                                        if (cell != null) {

//                                            String cellValue = cell.getStringCellValue();
                                            String cellValue;
                                            if (cell.getCellTypeEnum() == CellType.NUMERIC) {
                                                cellValue = String.valueOf(Math.round(cell.getNumericCellValue()));
                                            } else {
                                                cellValue = cell.getStringCellValue();
                                            }

                                            if (colNum + 1 == 3) {

                                                String getAbbreviation = "";
                                                String getNumberRemovedZeroLeft = "";

                                                if (cellValue.contains("-")) {
                                                    if (cellValue.length() > 1) {
                                                        getAbbreviation = cellValue.split("-")[0].trim();
                                                        getNumberRemovedZeroLeft = cellValue.split("-")[1].replaceAll("^0+", "");
                                                    }
                                                } else {
                                                    String valueOfCell = cellValue.trim();
                                                    getAbbreviation = valueOfCell.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeft = valueOfCell.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeft = getNumberRemovedZeroLeft.replaceAll("^0+", "");
                                                }
                                                String getFirstAndLastLetter = "";

                                                if (getAbbreviation.length() > 1) {
                                                    getFirstAndLastLetter = getAbbreviation.substring(0, 1) + getAbbreviation.substring(getAbbreviation.length() - 1);
                                                } else if (getAbbreviation.length() == 1) {
                                                    getFirstAndLastLetter = getAbbreviation + getAbbreviation;
                                                }

                                                XSSFCell cellE = row.getCell(colNum + 2);

                                                String cellValueCodeOldE;
                                                if (cellE.getCellTypeEnum() == CellType.NUMERIC) {
                                                    cellValueCodeOldE = String.valueOf(Math.round(cellE.getNumericCellValue()));
                                                } else {
                                                    cellValueCodeOldE = cellE.getStringCellValue();
                                                }
                                                String cellValueOldCodeE = cellValueCodeOldE;

                                                String getAbbreviationOldCodeE = "";
                                                String getNumberRemovedZeroLeftOldCodeE = "";

                                                if (cellValueOldCodeE.contains("-")) {

                                                    if (cellValueOldCodeE.length() > 1) {
                                                        getAbbreviationOldCodeE = cellValueOldCodeE.split("-")[0].trim();
                                                        getNumberRemovedZeroLeftOldCodeE = cellValueOldCodeE.split("-")[1].replaceAll("^0+", "");
                                                    }

                                                } else {
                                                    String valueOfCellOfOldCodeE = cellValueOldCodeE.trim();
                                                    getAbbreviationOldCodeE = valueOfCellOfOldCodeE.replaceAll("\\d+", "");
                                                    getNumberRemovedZeroLeftOldCodeE = valueOfCellOfOldCodeE.replaceAll("\\D+", "");
                                                    getNumberRemovedZeroLeftOldCodeE = getNumberRemovedZeroLeftOldCodeE.replaceAll("^0+", "");
                                                }

                                                String getFirstAndLastLetterOfOldCode = "";

                                                if (getAbbreviationOldCodeE.length() > 1) {
                                                    getFirstAndLastLetterOfOldCode = getAbbreviationOldCodeE.substring(0, 1) + getAbbreviationOldCodeE.substring(getAbbreviationOldCodeE.length() - 1);
                                                } else if (getAbbreviationOldCodeE.length() == 1) {
                                                    getFirstAndLastLetterOfOldCode = getAbbreviationOldCodeE + getAbbreviationOldCodeE;
                                                }

                                                for (Assets assets : assetsThatNotContainG) {

                                                    String getAbbreviationOfAssets;
                                                    String getNumberRemovedZeroLeftAssets;

                                                    String getAbbreviationOfAssetsOldCode;
                                                    String getNumberRemovedZeroLeftAssetsOldCode;

                                                    if (assets.getCode_new().contains("-")) {

                                                        getAbbreviationOfAssets = assets.getCode_new().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfCell = assets.getCode_new().trim();
                                                        getAbbreviationOfAssets = valueOfCell.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssets = assets.getCode_new().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssets = getNumberRemovedZeroLeftAssets.replaceAll("^0+", "");
                                                    }

                                                    if (assets.getCode_old().contains("-")) {

                                                        getAbbreviationOfAssetsOldCode = assets.getCode_old().split("-")[0].trim();
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().split("-")[1].replaceAll("^0+", "");

                                                    } else {
                                                        String valueOfAsset = assets.getCode_old().trim();
                                                        getAbbreviationOfAssetsOldCode = valueOfAsset.replaceAll("\\d+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = assets.getCode_old().replaceAll("\\D+", "");
                                                        getNumberRemovedZeroLeftAssetsOldCode = getNumberRemovedZeroLeftAssetsOldCode.replaceAll("^0+", "");
                                                    }

                                                    String getFirstAndLastLetterAssets = "";
                                                    String getFirstAndLastLetterAssetsOld = "";

                                                    if (getAbbreviationOfAssets.length() > 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets.substring(0, 1) + getAbbreviationOfAssets.substring(getAbbreviationOfAssets.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssets = getAbbreviationOfAssets + getAbbreviationOfAssets;
                                                    }

                                                    if (getAbbreviationOfAssetsOldCode.length() > 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode.substring(0, 1) + getAbbreviationOfAssetsOldCode.substring(getAbbreviationOfAssetsOldCode.length() - 1);
                                                    } else if (getAbbreviationOfAssets.length() == 1) {
                                                        getFirstAndLastLetterAssetsOld = getAbbreviationOfAssetsOldCode + getAbbreviationOfAssetsOldCode;
                                                    }

                                                    String joinToCompareFirst = getFirstAndLastLetter + getNumberRemovedZeroLeft;
                                                    String joinToCompareFirstCodeOld = getFirstAndLastLetterOfOldCode + getNumberRemovedZeroLeftOldCodeE;
                                                    String joinToCompareMainCodeNew = getFirstAndLastLetterAssets + getNumberRemovedZeroLeftAssets;
                                                    String joinToCompareMainCodeOld = getFirstAndLastLetterAssetsOld + getNumberRemovedZeroLeftAssetsOldCode;

                                                    if ((joinToCompareFirst.length() > 0 || joinToCompareFirstCodeOld.length() > 0) && joinToCompareMainCodeNew.length() > 0 || joinToCompareMainCodeOld.length() > 0) {

                                                        if (joinToCompareFirst.isEmpty()) {
                                                            joinToCompareFirst = "Empty1";
                                                        }

                                                        if (joinToCompareFirstCodeOld.isEmpty()) {
                                                            joinToCompareFirstCodeOld = "Empty2";
                                                        }

                                                        if (joinToCompareMainCodeNew.isEmpty()) {
                                                            joinToCompareMainCodeNew = "Empty3";
                                                        }

                                                        if (joinToCompareMainCodeOld.isEmpty()) {
                                                            joinToCompareMainCodeOld = "Empty4";
                                                        }

                                                        if (joinToCompareFirst.equals(joinToCompareMainCodeNew) || joinToCompareFirst.equals(joinToCompareMainCodeOld) || joinToCompareFirstCodeOld.equals(joinToCompareMainCodeNew) || joinToCompareFirstCodeOld.equals(joinToCompareMainCodeOld)) {

                                                            LevenshteinDistance levenshteinDistance = new LevenshteinDistance();

                                                            XSSFCell cellD = row.getCell(colNum + 1);
// Asset is used
                                                            XSSFCell cellI = row.getCell(colNum + 6);
                                                            XSSFCell cellJ = row.getCell(colNum + 7);

                                                            XSSFCell cellL = row.getCell(colNum + 9);
                                                            XSSFCell cellN = row.getCell(colNum + 11);
//
//// state asset
                                                            XSSFCell cellO = row.getCell(colNum + 12);
                                                            XSSFCell cellP = row.getCell(colNum + 13);

//// observations
                                                            XSSFCell cellR = row.getCell(colNum + 15);
//
                                                            String cellValueD = cellD.getStringCellValue();

                                                            int distanceA = levenshteinDistance.apply(cellValueD, assets.getType());
                                                            int distanceB = levenshteinDistance.apply(cellValueD, assets.getBrand());

                                                            int maxLengthA = Math.max(cellValueD.length(), assets.getType().length());
                                                            double similarityPercentageA = (1 - (double) distanceA / maxLengthA) * 100;

                                                            int maxLengthB = Math.max(cellValueD.length(), assets.getBrand().length());
                                                            double similarityPercentageB = (1 - (double) distanceB / maxLengthB) * 100;

                                                            int plusSimilarity = (int) Math.round(similarityPercentageA + similarityPercentageB);

                                                            if (plusSimilarity >= 7) {
                                                                System.err.println("Porcentaje de coincidencia");
                                                            } else {
                                                                System.err.println("NO Porcentaje de coincidencia");
                                                            }

                                                            if (assets.getState().contains("MAL") || assets.getObservations().contains("MAL") || assets.getObservations().contains("VIEJ") || assets.getObservations().contains("INUTIL") || assets.getObservations().contains("DESHUSO") || assets.getObservations().contains("DESUSO") || assets.getObservations().contains("NO SIRVE") || assets.getObservations().contains("DESGASTADO") || assets.getObservations().contains("SIN USO") || assets.getObservations().isEmpty() || assets.getLocation().contains("PATIO")) {
                                                                cellJ.setCellValue("NO");
                                                                cellJ.setCellStyle(cellStyle);
                                                            } else {
                                                                cellI.setCellValue("SI");
                                                                cellI.setCellStyle(cellStyle);
                                                            }

                                                            if (assets.getState().contains("MAL")) {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("REGULAR")) {
                                                                cellO.setCellValue("R");
                                                                cellO.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BUEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else if (assets.getState().contains("BIEN")) {
                                                                cellN.setCellValue("B");
                                                                cellN.setCellStyle(cellStyle);
                                                            } else {
                                                                cellP.setCellValue("M");
                                                                cellP.setCellStyle(cellStyle);
                                                            }

                                                            cellL.setCellValue("NO");
                                                            cellL.setCellStyle(cellStyle);

                                                            cellR.setCellValue(assets.getObservations());
                                                            cellR.setCellStyle(cellStyle);

                                                            XSSFCell cellQ = row.getCell(colNum + 14);
                                                            if (cellQ == null) {
                                                                cellQ = row.createCell(colNum + 14);
                                                            }
                                                            cellQ.setCellValue("ENCONTRADO");
                                                            cellQ.setCellStyle(cellStyleQGreen);

                                                            XSSFCell cellH = row.getCell(colNum + 5);

                                                            String IsUsed = cellI.getStringCellValue();
                                                            String NotUsed = cellJ.getStringCellValue();

                                                            if (!IsUsed.isEmpty() && !NotUsed.isEmpty()) {
                                                                cellH.setCellValue("SI");
                                                                cellH.setCellStyle(cellStyle);
                                                            } else {
                                                                cellH.setCellValue("NO");
                                                                cellH.setCellStyle(cellStyle);
                                                            }
                                                        } else {
                                                            XSSFCell cellQ = row.getCell(colNum + 14);

                                                            if (cellQ.getStringCellValue().length() == 0) {
                                                                cellQ.setCellValue("NO ENCONTRADO");
                                                                cellQ.setCellStyle(cellStyleQRed);

                                                            }

                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        }

                        default:
                            break;
                    }

                } catch (Exception e) {
                    System.err.println("error: " + e);
                }
            } else {

            }
        }

        downloadFile(workbookMain);
    }

    public XSSFWorkbook leerArchivo(String pathname) {
        XSSFWorkbook workbook = null;

        try (FileInputStream file = new FileInputStream(this.realPath() + pathname)) {
            workbook = new XSSFWorkbook(file);
        } catch (IOException e) {
            System.err.println("error: " + e);
        }

        return workbook;
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

    public void saveDataOfCompare() {
        XSSFWorkbook workbookMain = readFileMainCompare();
        XSSFSheet sheet = workbookMain.getSheetAt(0);

        int startRow = 3;
        int startColumn = 0;

        if (sheet != null) {
            try {
                for (int rowIndex = startRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);

                    String item = getStringCellValue(row, startColumn);
                    String amount = getStringCellValue(row, startColumn + 1);
                    String code = getStringCellValue(row, startColumn + 2);
                    String description = getStringCellValue(row, startColumn + 3);
                    String data = getStringCellValue(row, startColumn + 4);
                    String area = getStringCellValue(row, startColumn + 5);
                    String custodian = getStringCellValue(row, startColumn + 6);
                    String code_old = getStringCellValue(row, startColumn + 7);

                    byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);
                    byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
                    byte[] areaBytes = area.getBytes(StandardCharsets.UTF_8);
                    byte[] custodianBytes = custodian.getBytes(StandardCharsets.UTF_8);

                    String utf8Description = new String(descriptionBytes, StandardCharsets.UTF_8);
                    String utf8Data = new String(dataBytes, StandardCharsets.UTF_8);
                    String utf8Area = new String(areaBytes, StandardCharsets.UTF_8);
                    String utf8Custodian = new String(custodianBytes, StandardCharsets.UTF_8);

                    System.err.println("utf8Custodian: " + utf8Custodian);

                    newAssetCompare.setItem(item);
                    newAssetCompare.setAmount(amount);
                    newAssetCompare.setCode(code);
                    newAssetCompare.setDescription(utf8Description);
                    newAssetCompare.setData(utf8Data);
                    newAssetCompare.setArea(utf8Area);
                    newAssetCompare.setCustodian(utf8Custodian);
                    newAssetCompare.setCodeold(code_old);

                    assetsCompareFacade.create(newAssetCompare);
                }
            } catch (Exception e) {
                System.err.println("error:" + e);
            }
        } else {
            System.err.println("workbookMain or sheet is null");
        }

    }

    private String getStringCellValue(Row row, int columnIndex) {
        DataFormatter dataFormatter = new DataFormatter();
        Cell cell = row.getCell(columnIndex);

        if (cell != null) {
            String cellValue = dataFormatter.formatCellValue(cell);
            return cellValue != null ? cellValue.trim() : "";
        } else {
            return "";
        }
    }

    public void compare() {

        Workbook wb = new XSSFWorkbook();

        Map<String, CellStyle> styles = createStyles(wb);

        Sheet sheet = wb.createSheet("Paper1");
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);

        Row row = sheet.createRow(0);

        Cell cell = row.createCell(0);
        cell.setCellValue("ITEM");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(1);
        cell.setCellValue("CANTIDAD");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(2);
        cell.setCellValue("CODIGO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(3);
        cell.setCellValue("CODIGO ANTIGUO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(4);
        cell.setCellValue("DESCRIPCIÓN");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(5);
        cell.setCellValue("DATOS");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(6);
        cell.setCellValue("AREA");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(7);
        cell.setCellValue("NOMBRE");
        cell.setCellStyle(styles.get("general_center"));

        List<AssetsCompare> listAssetsCompare = assetsCompareFacade.findAllAssetsThatExistInAssetsCompare();

        int rowNumber = 1;

        for (AssetsCompare assetsCompare : listAssetsCompare) {

            row = sheet.createRow(rowNumber);

            cell = row.createCell(0);
            cell.setCellValue(assetsCompare.getItem());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(1);
            cell.setCellValue(assetsCompare.getAmount());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(2);
            cell.setCellValue(assetsCompare.getCode());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(3);
            cell.setCellValue(assetsCompare.getCodeold());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(4);
            cell.setCellValue(assetsCompare.getDescription());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(5);
            cell.setCellValue(assetsCompare.getData());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(6);
            cell.setCellValue(assetsCompare.getArea());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(7);
            cell.setCellValue(assetsCompare.getCustodian());
            cell.setCellStyle(styles.get("general_center"));

            rowNumber++;
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

        try (FileOutputStream fos = new FileOutputStream(uploadDirectory + "response.xlsx")) {
            wb.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadFile((XSSFWorkbook) wb);

    }

    public void compareFalse() {
        Workbook wb = new XSSFWorkbook();

        Map<String, CellStyle> styles = createStyles(wb);

        Sheet sheet = wb.createSheet("Paper1");
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);

        Row row = sheet.createRow(0);

        Cell cell = row.createCell(0);
        cell.setCellValue("ITEM");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(1);
        cell.setCellValue("CANTIDAD");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(2);
        cell.setCellValue("CODIGO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(3);
        cell.setCellValue("CODIGO ANTIGUO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(4);
        cell.setCellValue("DESCRIPCIÓN");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(5);
        cell.setCellValue("DATOS");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(6);
        cell.setCellValue("AREA");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(7);
        cell.setCellValue("NOMBRE");
        cell.setCellStyle(styles.get("general_center"));

        List<AssetsCompare> listAssetsCompare = assetsCompareFacade.findAllAssetsThatNotExistInAssetsCompare();

        int rowNumber = 1;

        for (AssetsCompare assetsCompare : listAssetsCompare) {

            row = sheet.createRow(rowNumber);

            cell = row.createCell(0);
            cell.setCellValue(assetsCompare.getItem());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(1);
            cell.setCellValue(assetsCompare.getAmount());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(2);
            cell.setCellValue(assetsCompare.getCode());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(3);
            cell.setCellValue(assetsCompare.getCodeold());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(4);
            cell.setCellValue(assetsCompare.getDescription());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(5);
            cell.setCellValue(assetsCompare.getData());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(6);
            cell.setCellValue(assetsCompare.getArea());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(7);
            cell.setCellValue(assetsCompare.getCustodian());
            cell.setCellStyle(styles.get("general_center"));

            rowNumber++;
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);

        try (FileOutputStream fos = new FileOutputStream(uploadDirectory + "response.xlsx")) {
            wb.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadFile((XSSFWorkbook) wb);

    }

    public void getAllValuesThatNotMatch() {
        Workbook wb = new XSSFWorkbook();

        Map<String, CellStyle> styles = createStyles(wb);

        Sheet sheet = wb.createSheet("Paper1");
        PrintSetup printSetup = sheet.getPrintSetup();
        printSetup.setLandscape(true);
        sheet.setFitToPage(true);
        sheet.setHorizontallyCenter(true);

        Row row = sheet.createRow(0);

        Cell cell = row.createCell(0);
        cell.setCellValue("CODIGO NUEVO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(1);
        cell.setCellValue("CODIGO ANTIGUO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(2);
        cell.setCellValue("TIPO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(3);
        cell.setCellValue("MARCA");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(4);
        cell.setCellValue("MODELO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(5);
        cell.setCellValue("NUMERO SERIE");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(6);
        cell.setCellValue("DESCRIPCIÓN");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(7);
        cell.setCellValue("ESTADO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(8);
        cell.setCellValue("UBICACIÓN");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(9);
        cell.setCellValue("CUSTODIO");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(10);
        cell.setCellValue("CAMPUS");
        cell.setCellStyle(styles.get("general_center"));

        cell = row.createCell(11);
        cell.setCellValue("OBSERVACIONES");
        cell.setCellStyle(styles.get("general_center"));

        List<Assets> listAssets = assetsFacade.findAllAssetsCompareThatNotMatch();

        int rowNumber = 1;
        for (Assets assets : listAssets) {

            row = sheet.createRow(rowNumber);

            cell = row.createCell(0);
            cell.setCellValue(assets.getCode_new());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(1);
            cell.setCellValue(assets.getCode_old());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(2);
            cell.setCellValue(assets.getType());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(3);
            cell.setCellValue(assets.getBrand());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(4);
            cell.setCellValue(assets.getModel());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(5);
            cell.setCellValue(assets.getSerial_number());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(6);
            cell.setCellValue(assets.getDescription());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(7);
            cell.setCellValue(assets.getState());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(8);
            cell.setCellValue(assets.getLocation());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(9);
            cell.setCellValue(assets.getCustodian());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(10);
            cell.setCellValue(assets.getCampus());
            cell.setCellStyle(styles.get("general_center"));

            cell = row.createCell(11);
            cell.setCellValue(assets.getObservations());
            cell.setCellStyle(styles.get("general_center"));

            rowNumber++;

        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.autoSizeColumn(2);
        sheet.autoSizeColumn(3);
        sheet.autoSizeColumn(4);
        sheet.autoSizeColumn(5);
        sheet.autoSizeColumn(6);
        sheet.autoSizeColumn(7);
        sheet.autoSizeColumn(8);
        sheet.autoSizeColumn(9);
        sheet.autoSizeColumn(10);
        sheet.autoSizeColumn(11);

        try (FileOutputStream fos = new FileOutputStream(uploadDirectory + "response.xlsx")) {
            wb.write(fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
        downloadFile((XSSFWorkbook) wb);
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

    /**
     * @return the newAsset
     */
    public Assets getNewAsset() {
        return newAsset;
    }

    /**
     * @param newAsset the newAsset to set
     */
    public void setNewAsset(Assets newAsset) {
        this.newAsset = newAsset;
    }

    /**
     * @return the newAssetCompare
     */
    public AssetsCompare getNewAssetCompare() {
        return newAssetCompare;
    }

    /**
     * @param newAssetCompare the newAssetCompare to set
     */
    public void setNewAssetCompare(AssetsCompare newAssetCompare) {
        this.newAssetCompare = newAssetCompare;
    }

    private static Map<String, CellStyle> createStyles(Workbook wb) {
        Map<String, CellStyle> styles = new HashMap<String, CellStyle>();

        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        styles.put("general_center", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setDataFormat((short) 1);
        styles.put("number_center", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        styles.put("custom_center", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        styles.put("general_left", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setDataFormat((short) 1);
        styles.put("number_left", style);

        style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
        styles.put("custom_left", style);

        return styles;
    }

    /**
     * @return the codeNew
     */
    public String getCodeNew() {
        return codeNew;
    }

    /**
     * @param codeNew the codeNew to set
     */
    public void setCodeNew(String codeNew) {
        this.codeNew = codeNew;
    }

    /**
     * @return the codeOld
     */
    public String getCodeOld() {
        return codeOld;
    }

    /**
     * @param codeOld the codeOld to set
     */
    public void setCodeOld(String codeOld) {
        this.codeOld = codeOld;
    }

    /**
     * @return the codeNewToCompare
     */
    public String getCodeNewToCompare() {
        return codeNewToCompare;
    }

    /**
     * @param codeNewToCompare the codeNewToCompare to set
     */
    public void setCodeNewToCompare(String codeNewToCompare) {
        this.codeNewToCompare = codeNewToCompare;
    }

    /**
     * @return the PATHNAME
     */
    public String getPATHNAME() {
        return PATHNAME;
    }

    /**
     * @param PATHNAME the PATHNAME to set
     */
    public void setPATHNAME(String PATHNAME) {
        this.PATHNAME = PATHNAME;
    }

}
