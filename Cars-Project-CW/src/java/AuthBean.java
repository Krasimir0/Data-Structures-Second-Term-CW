/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.annotation.Resource;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.*;

/**
 *
 * @author krasi
 */

@Named("authBean")
@SessionScoped
public class AuthBean implements Serializable {
     @Resource(lookup = "jdbcarsDB")
    private DataSource ds;
 
    private int customerId;
    private String name;
    private String email;
    private String password;
    
    public String getName() {
    return name;
}

public void setName(String name) {
    this.name = name;
}

public String getEmail() {
    return email;
}

public void setEmail(String email) {
    this.email = email;
}

public String getPassword() {
    return password;
}

public void setPassword(String password) {
    this.password = password;
}
     
    public String register() {
        if (name == null || name.trim().isEmpty()) {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Name is required",
            "Please enter your name."));
    return null;
}

if (!name.matches("[a-zA-Z ]+")) {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Invalid name",
            "Name should contain only letters."));
    return null;
}

if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Invalid email",
            "Please enter a valid email address."));
    return null;
}

if (password == null || password.length() < 6) {
    FacesContext.getCurrentInstance().addMessage(null,
        new FacesMessage(FacesMessage.SEVERITY_ERROR,
            "Weak password",
            "Password must be at least 6 characters long."));
    return null;
}

        try (Connection con = ds.getConnection()) {
 
            PreparedStatement check = con.prepareStatement(
                "SELECT * FROM Customer WHERE email = ?"
            );
            check.setString(1, email);
            ResultSet rs = check.executeQuery();
 
            if (rs.next()) {
                FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Registration Failed",
                    "An account with this email already exists."));
                return null; 
            }
 
            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Customer (customer_id, name, email, password, phone, address) VALUES (?, ?, ?, ?, ?, ?)"
            );
 
            int id = (int)(Math.random() * 10000);
            ps.setInt(1, id);
            ps.setString(2, name);
            ps.setString(3, email);
            ps.setString(4, password);
            ps.setString(5, "000000000");
            ps.setString(6, "N/A");
 
            ps.executeUpdate();
            
            FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO,
                "Registration Successful 🎉",
                "Your account has been created. Please log in."));
            
            return "/login?faces-redirect=true";
 
        } catch (Exception e) {
            e.printStackTrace();
            
            FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_ERROR,
                "Error",
                "Something went wrong. Please try again."));
        }
 
        return null;
    }
 
    public String login() {
        try (Connection con = ds.getConnection()) {
 
            PreparedStatement ps = con.prepareStatement(
                "SELECT * FROM Customer WHERE email = ? AND password = ?"
            );
 
            ps.setString(1, email);
            ps.setString(2, password);
 
            ResultSet rs = ps.executeQuery();
 
            if (rs.next()) {
                customerId = rs.getInt("customer_id"); 
                name = rs.getString("name");
                return "/index?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Incorrect email or password. Please try again.",
                    "Incorrect email or password. Please try again."));
            }
 
        } catch (Exception e) {
            e.printStackTrace();
        }
 
        return null; 
    }
 
    public String logout() {
        customerId = 0;
        name = null;
        email = null;
        password = null;
        return "/login?faces-redirect=true";
    }
    
    public boolean isLoggedIn() {
    return customerId != 0;
}
}
