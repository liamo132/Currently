/*
 * File: ApplianceService.java
 * Description: Loads and serves appliance metadata from appliances.json.
 * Author: Liam Connell
 * Date: 2025-11-12
 */

package com.currently.currently_backend.service;

import com.currently.currently_backend.model.Appliance;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApplianceService {

    private final List<Appliance> appliances = new ArrayList<>();

    @PostConstruct
    public void loadAppliances() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // FIXED PATH: resources/appliances/appliances.json
            InputStream is = getClass().getResourceAsStream("/appliances/appliances.json");

            if (is == null) {
                throw new RuntimeException("appliances.json not found in resources/appliances/");
            }

            List<Appliance> loaded = mapper.readValue(is, new TypeReference<List<Appliance>>() {});
            appliances.clear();
            appliances.addAll(loaded);

            System.out.println("Loaded appliances: " + appliances.size());

        } catch (Exception e) {
            throw new RuntimeException("Failed to load appliances.json", e);
        }
    }

    public List<Appliance> getAllAppliances() {
        return appliances;
    }
}
