/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.entities;

import java.io.Serializable;
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
@Table(name = "assets", catalog = "findkme", schema = "public")
public class Assets implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id_asset;

    private String code_new;
    private String code_old;
    private String type;
    private String brand;
    private String model;
    private String serial_number;
    private String description;
    private String state;
    private String location;
    private String custodian;
    private String campus;
    private String observations;
    private String hostname;
    private String career;

    public Assets() {
    }

    /**
     * @return the code_new
     */
    public String getCode_new() {
        return code_new;
    }

    /**
     * @param code_new the code_new to set
     */
    public void setCode_new(String code_new) {
        this.code_new = code_new;
    }

    /**
     * @return the code_old
     */
    public String getCode_old() {
        return code_old;
    }

    /**
     * @param code_old the code_old to set
     */
    public void setCode_old(String code_old) {
        this.code_old = code_old;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the brand
     */
    public String getBrand() {
        return brand;
    }

    /**
     * @param brand the brand to set
     */
    public void setBrand(String brand) {
        this.brand = brand;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the serial_number
     */
    public String getSerial_number() {
        return serial_number;
    }

    /**
     * @param serial_number the serial_number to set
     */
    public void setSerial_number(String serial_number) {
        this.serial_number = serial_number;
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
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
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
     * @return the campus
     */
    public String getCampus() {
        return campus;
    }

    /**
     * @param campus the campus to set
     */
    public void setCampus(String campus) {
        this.campus = campus;
    }

    /**
     * @return the observations
     */
    public String getObservations() {
        return observations;
    }

    /**
     * @param observations the observations to set
     */
    public void setObservations(String observations) {
        this.observations = observations;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the career
     */
    public String getCareer() {
        return career;
    }

    /**
     * @param career the career to set
     */
    public void setCareer(String career) {
        this.career = career;
    }

    /**
     * @return the id_asset
     */
    public Integer getId_asset() {
        return id_asset;
    }

    /**
     * @param id_asset the id_asset to set
     */
    public void setId_asset(Integer id_asset) {
        this.id_asset = id_asset;
    }

}
