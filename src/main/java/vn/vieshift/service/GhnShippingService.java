package vn.vieshift.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.*;

/**
 * Tích hợp API Giao Hàng Nhanh (GHN) & Định vị GPS Reverse Geocoding
 * Kế thừa nguyên bản từ Services\GHNService.php của repo LTW_01 (Minishop_LeThanhPVu)
 */
@Service
public class GhnShippingService {

    @Value("${vieshift.ghn.token:1824adf2-9f85-11f1-b30e-92bc095bb794}")
    private String ghnToken;

    @Value("${vieshift.ghn.shop-id:6625151}")
    private int ghnShopId;

    private static final String BASE_URL = "https://online-gateway.ghn.vn/shiip/public-api/";
    private static final int FROM_DISTRICT_ID = 1442; // Kho chính: Quận 1, TP.HCM
    private static final String FROM_WARD_CODE = "20101";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GhnShippingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(6))
                .build();
    }

    private JsonNode sendGhnRequest(String endpoint, Map<String, Object> payload, String method) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + endpoint))
                    .timeout(Duration.ofSeconds(8))
                    .header("Token", ghnToken)
                    .header("ShopId", String.valueOf(ghnShopId))
                    .header("Content-Type", "application/json");

            if ("POST".equalsIgnoreCase(method)) {
                String jsonBody = objectMapper.writeValueAsString(payload != null ? payload : Collections.emptyMap());
                builder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readTree(response.body());
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Lấy danh sách 63 Tỉnh/Thành từ GHN (Ưu tiên TP.HCM 202 và Hà Nội 201 lên đầu giống LTW_01)
     */
    public List<Map<String, Object>> getProvinces() {
        JsonNode root = sendGhnRequest("master-data/province", null, "GET");
        List<Map<String, Object>> list = new ArrayList<>();
        if (root != null && root.path("code").asInt() == 200 && root.path("data").isArray()) {
            for (JsonNode p : root.path("data")) {
                if (p.path("IsEnable").asInt(1) == 1) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", p.path("ProvinceID").asInt());
                    item.put("name", p.path("ProvinceName").asText());
                    list.add(item);
                }
            }
            list.sort((a, b) -> {
                int idA = (int) a.get("id");
                int idB = (int) b.get("id");
                if (idA == 202) return -1;
                if (idB == 202) return 1;
                if (idA == 201) return -1;
                if (idB == 201) return 1;
                return String.valueOf(a.get("name")).compareTo(String.valueOf(b.get("name")));
            });
            return list;
        }

        // Fallback dự phòng nếu mất kết nối mạng ngoại vi
        list.add(Map.of("id", 202, "name", "Hồ Chí Minh"));
        list.add(Map.of("id", 201, "name", "Hà Nội"));
        list.add(Map.of("id", 203, "name", "Đà Nẵng"));
        list.add(Map.of("id", 205, "name", "Bình Dương"));
        list.add(Map.of("id", 204, "name", "Đồng Nai"));
        list.add(Map.of("id", 220, "name", "Cần Thơ"));
        return list;
    }

    /**
     * Lấy danh sách Quận/Huyện theo Province ID từ GHN
     */
    public List<Map<String, Object>> getDistricts(int provinceId) {
        JsonNode root = sendGhnRequest("master-data/district", Map.of("province_id", provinceId), "POST");
        List<Map<String, Object>> list = new ArrayList<>();
        if (root != null && root.path("code").asInt() == 200 && root.path("data").isArray()) {
            for (JsonNode d : root.path("data")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", d.path("DistrictID").asInt());
                item.put("name", d.path("DistrictName").asText());
                list.add(item);
            }
            list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
            return list;
        }

        // Fallback cho TP.HCM (202)
        if (provinceId == 202) {
            list.add(Map.of("id", 1442, "name", "Quận 1"));
            list.add(Map.of("id", 1444, "name", "Quận 3"));
            list.add(Map.of("id", 1462, "name", "Quận Bình Thạnh"));
            list.add(Map.of("id", 1449, "name", "Quận 7"));
            list.add(Map.of("id", 3695, "name", "Thành phố Thủ Đức"));
        } else {
            list.add(Map.of("id", 1484, "name", "Quận Trung Tâm"));
        }
        return list;
    }

    /**
     * Lấy danh sách Phường/Xã theo District ID từ GHN
     */
    public List<Map<String, Object>> getWards(int districtId) {
        JsonNode root = sendGhnRequest("master-data/ward", Map.of("district_id", districtId), "POST");
        List<Map<String, Object>> list = new ArrayList<>();
        if (root != null && root.path("code").asInt() == 200 && root.path("data").isArray()) {
            for (JsonNode w : root.path("data")) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("code", w.path("WardCode").asText());
                item.put("name", w.path("WardName").asText());
                list.add(item);
            }
            list.sort(Comparator.comparing(a -> String.valueOf(a.get("name"))));
            return list;
        }

        list.add(Map.of("code", "20101", "name", "Phường Bến Nghé"));
        list.add(Map.of("code", "21722", "name", "Phường 22"));
        list.add(Map.of("code", "20102", "name", "Phường Bến Thành"));
        return list;
    }

    /**
     * Tính phí vận chuyển GHN thực tế + Áp dụng chính sách Trợ giá / Freeship (giống LTW_01)
     */
    public Map<String, Object> calculateShippingFee(int toDistrictId, String toWardCode,
                                                    int totalItems, BigDecimal cartSubtotal, int serviceTypeId) {
        int weight = Math.max(200, totalItems * 500);
        int insurance = cartSubtotal != null ? Math.min(5000000, cartSubtotal.intValue()) : 0;
        int svcType = (serviceTypeId == 1 || serviceTypeId == 2) ? serviceTypeId : 2;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("from_district_id", FROM_DISTRICT_ID);
        payload.put("from_ward_code", FROM_WARD_CODE);
        payload.put("to_district_id", toDistrictId);
        payload.put("to_ward_code", toWardCode != null ? toWardCode : "");
        payload.put("service_type_id", svcType);
        payload.put("weight", weight);
        payload.put("length", 15);
        payload.put("width", 15);
        payload.put("height", 10);
        payload.put("insurance_value", insurance);

        BigDecimal rawFee = new BigDecimal("28000");
        String serviceName = (svcType == 2) ? "🚀 GHN Giao Nhanh Chuẩn" : "⚡ GHN Hỏa Tốc";

        if (toDistrictId > 0 && toWardCode != null && !toWardCode.isBlank()) {
            JsonNode root = sendGhnRequest("v2/shipping-order/fee", payload, "POST");
            if (root != null && root.path("code").asInt() == 200 && root.path("data").has("total")) {
                rawFee = BigDecimal.valueOf(root.path("data").path("total").asLong(28000));
            }
        }

        // Chính sách Freeship / Trợ giá từ LTW_01: Đơn từ 300.000đ được trợ giá tối đa 30.000đ (Miễn phí nội thành)
        BigDecimal subtotal = cartSubtotal != null ? cartSubtotal : BigDecimal.ZERO;
        BigDecimal subsidy = BigDecimal.ZERO;
        boolean freeshipApplied = false;

        if (subtotal.compareTo(ShopService.FREESHIP_THRESHOLD) >= 0 && rawFee.compareTo(BigDecimal.ZERO) > 0) {
            freeshipApplied = true;
            BigDecimal maxSubsidy = new BigDecimal("35000");
            subsidy = rawFee.min(maxSubsidy);
        }

        BigDecimal finalFee = rawFee.subtract(subsidy).max(BigDecimal.ZERO);
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));

        String formatted;
        if (freeshipApplied && finalFee.compareTo(BigDecimal.ZERO) == 0) {
            formatted = "MIỄN PHÍ (GHN Trợ giá -" + nf.format(subsidy) + " đ)";
        } else if (freeshipApplied && subsidy.compareTo(BigDecimal.ZERO) > 0) {
            formatted = nf.format(finalFee) + " đ (Đã trợ giá -" + nf.format(subsidy) + " đ)";
        } else {
            formatted = nf.format(finalFee) + " đ";
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("rawFee", rawFee);
        result.put("subsidy", subsidy);
        result.put("shippingFee", finalFee);
        result.put("shippingFeeFormatted", formatted);
        result.put("serviceName", serviceName);
        result.put("isFreeship", freeshipApplied);
        return result;
    }

    /**
     * Định vị GPS Reverse Geocode (tương đương CartController::geocode_reverse trong LTW_01)
     */
    public Map<String, Object> reverseGeocode(double lat, double lon) {
        Map<String, Object> res = new LinkedHashMap<>();
        try {
            String url = String.format(Locale.US,
                    "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=%f&longitude=%f&localityLanguage=vi",
                    lat, lon);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            JsonNode data = objectMapper.readTree(response.body());

            String city = data.path("principalSubdivision").asText(data.path("city").asText("Hồ Chí Minh"));
            String locality = data.path("locality").asText("");
            String district = "";

            JsonNode informative = data.path("localityInfo").path("informative");
            if (informative.isArray()) {
                for (JsonNode info : informative) {
                    String name = info.path("name").asText("");
                    if (name.contains("Quận") || name.contains("Huyện") || name.contains("Thành phố")) {
                        district = name;
                        break;
                    }
                }
            }
            if (district.isEmpty()) {
                JsonNode admin = data.path("localityInfo").path("administrative");
                if (admin.isArray()) {
                    for (JsonNode ad : admin) {
                        String name = ad.path("name").asText("");
                        if (name.contains("Quận") || name.contains("Huyện")) {
                            district = name;
                            break;
                        }
                    }
                }
            }

            String street = !locality.isBlank() ? "Vị trí GPS " + locality : "Vị trí định vị GPS";
            String displayName = street + (!district.isBlank() ? ", " + district : "") + (!city.isBlank() ? ", " + city : "");

            res.put("success", true);
            res.put("city", city);
            res.put("district", district);
            res.put("ward", locality);
            res.put("street", street);
            res.put("displayName", displayName);
            return res;
        } catch (Exception e) {
            res.put("success", false);
            res.put("message", "Không thể kết nối máy chủ bản đồ GPS.");
            return res;
        }
    }
}
