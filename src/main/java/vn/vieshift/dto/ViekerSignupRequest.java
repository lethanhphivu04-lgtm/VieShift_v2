package vn.vieshift.dto;

import jakarta.validation.constraints.*;

/** Bước 1 đăng ký đối tác VieKer (tương tự "Create your account" của TaskRabbit). */
public class ViekerSignupRequest {

    @NotBlank(message = "Vui lòng nhập Email")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Vui lòng nhập họ")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Vui lòng nhập tên")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Vui lòng tạo mật khẩu")
    @Size(min = 8, message = "Mật khẩu tối thiểu 8 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng chọn khu vực hoạt động")
    private String area;

    @NotBlank(message = "Vui lòng chọn lĩnh vực")
    private String category;

    @AssertTrue(message = "Bạn cần xác nhận hoạt động với tư cách cá nhân kinh doanh độc lập")
    private boolean soleProprietor;

    @AssertTrue(message = "Bạn cần đồng ý với Điều khoản và Thỏa thuận đối tác")
    private boolean agreeTerms;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public boolean isSoleProprietor() { return soleProprietor; }
    public void setSoleProprietor(boolean soleProprietor) { this.soleProprietor = soleProprietor; }
    public boolean isAgreeTerms() { return agreeTerms; }
    public void setAgreeTerms(boolean agreeTerms) { this.agreeTerms = agreeTerms; }
}
