package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.math3.util.FastMath;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendLocation;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;

@Component
public class BotConfig extends TelegramLongPollingBot {

    private final String token;
    private final String username;

    private static final String API_KEY="write api key";
    private static final String URL="https://api.collectapi.com/health/dutyPharmacy?ilce=";

    private final RestTemplateBuilder restTemplate;
    private final ObjectMapper objectMapper;

    public BotConfig(@Value("${telegram.bot.token}") String token, @Value("${telegram.bot.username}") String username, RestTemplateBuilder restTemplate, ObjectMapper objectMapper) {
        this.token = token;
        this.username = username;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }


    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage()) {

            Message message = update.getMessage();
            Long chatId = message.getChatId();


            String request_url = URL  + "&il=" + "Ankara";
            HttpHeaders headers=new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("authorization",API_KEY);
            HttpEntity<Void> requestEntity=new HttpEntity<>(headers);
            ResponseEntity<String> response =restTemplate.build().exchange(request_url, HttpMethod.GET,requestEntity, String.class);

            double myLatitude = update.getMessage().getLocation().getLatitude(); // Kendi enlem (latitude)
            double myLongitude = update.getMessage().getLocation().getLongitude(); // Kendi boylam (longitude)
            String nearestPharmacyName = null;
            double minDistance = Double.MAX_VALUE;
            double pharmacyLatitudeTemp = Double.MAX_VALUE;
            double pharmacyLongitudeTemp = Double.MAX_VALUE;

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode rootNode = mapper.readTree(response.getBody());

                JsonNode resultNode = rootNode.get("result");
                if (resultNode.isArray()) {

                    for (JsonNode pharmacyNode : resultNode) {
                        String loc = pharmacyNode.get("loc").asText();
                        String[] locParts = loc.split(",");
                        if (locParts.length == 2) {
                            double pharmacyLatitude = Double.parseDouble(locParts[0]);
                            double pharmacyLongitude = Double.parseDouble(locParts[1]);

                            // Haversine formülü ile mesafeyi hesapla
                            double distance = calculateDistance(myLatitude, myLongitude, pharmacyLatitude, pharmacyLongitude);

                            // En yakın eczaneyi bul
                            if (distance < minDistance) {
                                minDistance = distance;
                                nearestPharmacyName = pharmacyNode.get("name").asText();
                                pharmacyLatitudeTemp = pharmacyLatitude;
                                pharmacyLongitudeTemp = pharmacyLongitude;
                            }
                        }
                    }

                    // Sonuçları ekrana yazdır
                    if (nearestPharmacyName != null) {
                        /*
                        System.out.println("En yakın eczane: " + nearestPharmacyName);
                        System.out.println("Mesafe (km): " + minDistance);
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("Adı:", nearestPharmacyName);
                        jsonObject.put("Uzaklığı (km):", minDistance);
                        jsonObject.put("Lokasyon:", pharmacyLatitudeTemp + ", " + pharmacyLongitudeTemp);
                        jsonObject.put("Link:", "https://www.google.com/maps/search/?api=1&query=" + pharmacyLatitudeTemp + ", " + pharmacyLongitudeTemp);

                         */

                        SendMessage sendResponse = new SendMessage();
                        sendResponse.setChatId(String.valueOf(chatId));
                        sendResponse.setText("En yakın eczane: " + nearestPharmacyName + " ECZANESİ");
                        execute(sendResponse);

                        SendMessage sendResponse1 = new SendMessage();
                        sendResponse1.setChatId(String.valueOf(chatId));
                        double minDistanceTemp = minDistance;
                        sendResponse1.setText("Mesafe: " + minDistanceTemp + " km");
                        execute(sendResponse1);

                        SendLocation sendLocation = new SendLocation();
                        sendLocation.setChatId(String.valueOf(chatId));
                        sendLocation.setLatitude(pharmacyLatitudeTemp);
                        sendLocation.setLongitude(pharmacyLongitudeTemp);
                        execute(sendLocation);
                    } else {
                        System.out.println("Yakın eczane bulunamadı.");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }


/*
            try {
                // execute(response);
                execute(sendLocation);


            } catch (TelegramApiException e) {

            }

 */
        }
    }

    private static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Dünya yarıçapı (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = FastMath.sin(dLat / 2) * FastMath.sin(dLat / 2) +
                FastMath.cos(Math.toRadians(lat1)) * FastMath.cos(Math.toRadians(lat2)) *
                        FastMath.sin(dLon / 2) * FastMath.sin(dLon / 2);
        double c = 2 * FastMath.atan2(FastMath.sqrt(a), FastMath.sqrt(1 - a));
        return R * c; // Mesafe (km)
    }


    @PostConstruct
    public void start() {
        System.out.println(getBotUsername() + " started Successfully");
    }
}
