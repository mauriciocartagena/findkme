/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.entities;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 *
 * @author Maverick-
 */
@Entity
@Table(name = "assetsCompare", catalog = "findkme", schema = "public")
public class AssetsCompare implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_asset_compare;

    @Column(name = "item", length = 5000)
    private String item;
    @Column(name = "amount", length = 5000)
    private String amount;
    @Column(name = "code", length = 5000)
    private String code;
    @Column(name = "codeold", length = 5000)
    private String codeold;
    @Column(name = "description", length = 5000)
    private String description;
    @Column(name = "data", length = 5000)
    private String data;
    @Column(name = "area", length = 5000)
    private String area;
    @Column(name = "custodian", length = 5000)
    private String custodian;

    public AssetsCompare() {
    }

    /**
     * @return the id_asset_compare
     */
    public Integer getId_asset_compare() {
        return id_asset_compare;
    }

    /**
     * @param id_asset_compare the id_asset_compare to set
     */
    public void setId_asset_compare(Integer id_asset_compare) {
        this.id_asset_compare = id_asset_compare;
    }

    /**
     * @return the item
     */
    public String getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(String item) {
        this.item = item;
    }

    /**
     * @return the amount
     */
    public String getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(String amount) {
        this.amount = amount;
    }

    /**
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * @param code the code to set
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the data
     */
    public String getData() {
        return data;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * @return the area
     */
    public String getArea() {
        return area;
    }

    /**
     * @param area the area to set
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * @return the custodian
     */
    public String getCustodian() {
        return custodian;
    }

    /**
     * @param custodian the custodian to set
     */
    public void setCustodian(String custodian) {
        this.custodian = custodian;
    }

    /**
     * @return the codeold
     */
    public String getCodeold() {
        return codeold;
    }

    /**
     * @param codeold the codeold to set
     */
    public void setCodeold(String codeold) {
        this.codeold = codeold;
    }

}
