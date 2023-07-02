/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.findkme.facades;

import com.findkme.entities.Assets;
import com.findkme.entities.AssetsCompare;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author Maverick-
 */
@Stateless
@LocalBean
public class AssetsFacade extends AbstractFacade<Assets> {

    @PersistenceContext(unitName = "findkme")
    private EntityManager em;

    public AssetsFacade() {
        super(Assets.class);
    }

    @Override
    public EntityManager getEntityManager() {
        return em;
    }

    public List<Assets> findAllAssetsCompareThatNotMatch() {
        List<Assets> l = new ArrayList();

        try {
            Query q = em.createNativeQuery(" SELECT\n"
                    + "	a.*\n"
                    + "FROM\n"
                    + "	public.assets AS a "
                    + " WHERE "
                    + "	NOT EXISTS (\n"
                    + "		SELECT\n"
                    + "			1\n"
                    + "		FROM\n"
                    + "		\"public\".assetscompare AS ac\n"
                    + "		WHERE (a.code_new NOT LIKE '%G1%'\n"
                    + "			AND a.code_new NOT LIKE '%G2%'\n"
                    + "			AND a.code_new NOT LIKE '%G3%'\n"
                    + "			AND a.code_new NOT LIKE '%G4%'\n"
                    + "			AND a.code_new NOT LIKE '%G5%')\n"
                    + "			AND (\n"
                    + "				\n"
                    + "				TRIM(\n"
                    + "						LEADING '0' FROM CAST(\n"
                    + "							CASE WHEN REGEXP_REPLACE(SUBSTRING(a.code_new FROM '-' || '.+'), '[^0-9]', '') IS NULL THEN\n"
                    + "								REGEXP_REPLACE(SUBSTRING(a.code_new FROM '' || '.+'), '[^0-9]', '')\n"
                    + "							ELSE\n"
                    + "								REGEXP_REPLACE(SUBSTRING(a.code_new FROM '-' || '.+'), '[^0-9]', '')\n"
                    + "							END\n"
                    + "							AS TEXT\n"
                    + "						)\n"
                    + "					)\n"
                    + "				=\n"
                    + "				TRIM(\n"
                    + "						LEADING '0' FROM CAST(\n"
                    + "							CASE WHEN REGEXP_REPLACE(SUBSTRING(ac.code FROM '-' || '.+'), '[^0-9]', '') IS NULL THEN\n"
                    + "								REGEXP_REPLACE(SUBSTRING(ac.code FROM '' || '.+'), '[^0-9]', '')\n"
                    + "							ELSE\n"
                    + "								REGEXP_REPLACE(SUBSTRING(ac.code FROM '-' || '.+'), '[^0-9]', '')\n"
                    + "							END\n"
                    + "							AS TEXT\n"
                    + "						)\n"
                    + "					)\n"
                    + "				OR\n"
                    + "				TRIM(\n"
                    + "						LEADING '0' FROM CAST(\n"
                    + "							CASE WHEN REGEXP_REPLACE(SUBSTRING(a.code_old FROM '-' || '.+'), '[^0-9]', '') IS NULL THEN\n"
                    + "								REGEXP_REPLACE(SUBSTRING(a.code_old FROM '' || '.+'), '[^0-9]', '')\n"
                    + "							ELSE\n"
                    + "								REGEXP_REPLACE(SUBSTRING(a.code_old FROM '-' || '.+'), '[^0-9]', '')\n"
                    + "							END\n"
                    + "							AS TEXT\n"
                    + "						)\n"
                    + "					)\n"
                    + "				=\n"
                    + "				TRIM(\n"
                    + "						LEADING '0' FROM CAST(\n"
                    + "							CASE WHEN REGEXP_REPLACE(SUBSTRING(ac.codeold FROM '-' || '.+'), '[^0-9]', '') IS NULL THEN\n"
                    + "								REGEXP_REPLACE(SUBSTRING(ac.codeold FROM '' || '.+'), '[^0-9]', '')\n"
                    + "							ELSE\n"
                    + "								REGEXP_REPLACE(SUBSTRING(ac.codeold FROM '-' || '.+'), '[^0-9]', '')\n"
                    + "							END\n"
                    + "							AS TEXT\n"
                    + "						)\n"
                    + "					))\n"
                    + "				OR\n"
                    + "				(TRIM(LEADING '0' FROM CAST(SUBSTRING(a.code_old FROM '^(\\\\d+)') AS TEXT))\n"
                    + "					= TRIM(LEADING '0' FROM CAST(REGEXP_REPLACE(SUBSTRING(ac.code FROM '-' || '.+'), '[^0-9]', '') AS TEXT)))\n"
                    + "					OR\n"
                    + "					(TRIM(LEADING '0' FROM CAST(SUBSTRING(a.code_old FROM '^(\\\\d+)') AS TEXT))\n"
                    + "					= TRIM(LEADING '0' FROM CAST(SUBSTRING(ac.codeold FROM '^(\\\\d+)') AS TEXT)))\n"
                    + "					OR\n"
                    + "					(TRIM(LEADING '0' FROM CAST(REGEXP_REPLACE(SUBSTRING(a.code_old FROM '-' || '.+'), '[^0-9]', '') AS TEXT))\n"
                    + "					= TRIM(LEADING '0' FROM CAST(SUBSTRING(ac.codeold FROM '^(\\\\d+)') AS TEXT)))\n"
                    + "				 AND  (CONCAT(LEFT(a.code_new , 1), SUBSTRING(a.code_new, POSITION('-' IN a.code_new) - 1, 1)) = CONCAT(LEFT(ac.code, 1), SUBSTRING(ac.code, POSITION('-' IN ac.code) - 1, 1))\n"
                    + "					  OR CONCAT (LEFT(REGEXP_REPLACE(a.code_old, '[^A-Z]+', ''), 1), RIGHT(REGEXP_REPLACE(a.code_old, '[^A-Z]+', ''), 1)) = CONCAT(LEFT(ac.code, 1), SUBSTRING(ac.code, POSITION('-' IN ac.code) - 1, 1))\n"
                    + "					  )\n"
                    + "					  )\n"
                    + "", Assets.class);

            l = q.getResultList();
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }

        return l;
    }

    public List<Assets> findAllAssets() {
        List<Assets> l = new ArrayList();

        try {
            Query q = em.createNativeQuery(" SELECT "
                    + "	a.* "
                    + " FROM "
                    + "	public.assets AS a "
                    + "	 WHERE (a.code_new NOT LIKE '%G1%' "
                    + "		AND a.code_new NOT LIKE '%G2%' "
                    + "		AND a.code_new NOT LIKE '%G3%' "
                    + "		AND a.code_new NOT LIKE '%G4%' "
                    + "		AND a.code_new NOT LIKE '%G5%') "
                    + "			"
                    + "", Assets.class);

            l = q.getResultList();
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }

        return l;
    }

}
