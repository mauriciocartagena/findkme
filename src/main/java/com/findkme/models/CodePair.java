/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.models;

import javax.inject.Named;

/**
 *
 * @author Maverick-
 */
@Named("CodePair")
public class CodePair {

    private String codeNew;
    private String codeOld;

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
}
