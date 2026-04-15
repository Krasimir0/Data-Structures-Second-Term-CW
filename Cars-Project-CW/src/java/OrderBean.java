/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author krasi
 */
import jakarta.inject.Named;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.annotation.Resource;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

import javax.sql.DataSource;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Named("orderBean")
@SessionScoped
public class OrderBean implements Serializable {

    @Resource(lookup = "jdbc/CarsDB")
    private DataSource ds;

    @Inject
    private AuthBean authBean;

    public String buyCar(int carId, double price) {

        if (authBean == null || authBean.getCustomerId() == 0) {
            return "/login?faces-redirect=true";
        }

        try (Connection con = ds.getConnection()) {

            PreparedStatement ps = con.prepareStatement(
                "INSERT INTO Orders (order_date, total_amount, status, car_id, customer_id) " +
                "VALUES (?, ?, ?, ?, ?)"
            );

            ps.setDate(1, new java.sql.Date(System.currentTimeMillis()));
            ps.setDouble(2, price);
            ps.setString(3, "PENDING");
            ps.setInt(4, carId);
            ps.setInt(5, authBean.getCustomerId());

            ps.executeUpdate();

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO,
                    "Order Created 🎉",
                    "Your order has been placed successfully."));

            return "/Pages/orders/List?faces-redirect=true";

        } catch (SQLException e) {
            e.printStackTrace();

            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR,
                    "Order Failed",
                    "Could not create order. Please try again."));
        }

        return null;
    }
}
