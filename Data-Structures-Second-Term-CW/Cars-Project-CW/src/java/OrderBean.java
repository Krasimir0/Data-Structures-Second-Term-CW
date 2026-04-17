/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author krasi
 */
import entities.OrdersController;
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

    @Resource(lookup = "jdbcarsDB")
    private DataSource ds;

    @Inject
    private AuthBean authBean;

    @Inject
    private entities.CarController carController;

    @Inject
    private OrdersController ordersController;

    public String buyCar(int carId, double price, String car_name) {

        if (authBean == null || authBean.getCustomerId() == 0) {
            return "/login?faces-redirect=true";
        }

        try (Connection con = ds.getConnection()) {

            int orderId = (int) (Math.random() * 1_000_000);

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO Orders (order_id, order_date, total_amount, status, car_id, customer_id, car_name)"
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)"
            );

            ps.setInt(1, orderId);
            ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
            ps.setDouble(3, price);
            ps.setString(4, "PENDING");
            ps.setInt(5, carId);
            ps.setInt(6, authBean.getCustomerId());
            ps.setString(7, car_name);

            ps.executeUpdate();

            PreparedStatement updateCar = con.prepareStatement(
                    "UPDATE Car SET status = ? WHERE car_id = ?"
            );
            updateCar.setString(1, "SOLD");
            updateCar.setInt(2, carId);
            updateCar.executeUpdate();


            carController.refresh();
            ordersController.refresh();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Order Created 🎉",
                            "Your order #" + orderId + " has been placed."));

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

    public String cancelOrder(Integer orderId, Integer carId) {

        try (Connection con = ds.getConnection()) {

            // 1. Delete the order
            PreparedStatement ps = con.prepareStatement(
                    "DELETE FROM Orders WHERE order_id = ?"
            );
            ps.setInt(1, orderId);
            ps.executeUpdate();

            // 2. Make car available again
            PreparedStatement ps2 = con.prepareStatement(
                    "UPDATE Car SET status = 'AVAILABLE' WHERE car_id = ?"
            );
            ps2.setInt(1, carId);
            ps2.executeUpdate();

            carController.refresh();
            ordersController.refresh();

            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Order Cancelled ❌",
                            "The car is now available again."));

            return "/Pages/orders/List?faces-redirect=true";

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
