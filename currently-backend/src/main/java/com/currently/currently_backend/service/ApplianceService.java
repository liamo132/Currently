/*
 * File: ApplianceService.java
 * Description: Loads and serves appliance metadata from appliances.json.
 * Project: Currently
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

    /*
     * Service: Appliance catalogue load
     * Purpose: Loads static appliance metadata once when Spring starts.
     * Output: Populates an in-memory list used for Appliance Usage, Cost, and Watch Your Watts calculations.
     */
    @PostConstruct
    public void loadAppliances() {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // Important logic: read the appliance catalogue packaged under src/main/resources.
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

    // Service: returns the loaded Appliance catalogue to controllers and energy calculation services.
    public List<Appliance> getAllAppliances() {
        return appliances;
    }
}
