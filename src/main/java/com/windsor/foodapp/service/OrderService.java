package com.windsor.foodapp.service;

import com.windsor.foodapp.dao.FoodItemDao;
import com.windsor.foodapp.dao.OrderDao;
import com.windsor.foodapp.dao.UserDao;
import com.windsor.foodapp.enums.ORDER_STATUS_ENUM;
import com.windsor.foodapp.model.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class OrderService {

    @Resource
    FoodItemDao foodItemDao;

    @Resource
    UserDao userDao;

    @Resource
    OrderDao orderDao;


    public CustomerOrder placeOrder(String email, Map<Integer, Integer> foodIdToQuantityMap) {
        String ids = foodIdToQuantityMap.keySet().stream().map(Object::toString).collect(Collectors.joining(","));

        //get user name
        ClientUser user = userDao.getUserForEmail(email);
        Map<Integer, FoodItem> foodIdtoItemMap = foodItemDao.getFoodItemsForIds(ids);
        //first need to save order so that order id can be generated
        double totalCost = 0;
        long maxTime =0;
        //Creating order calculation
        int fcId = 0;
        String fcName ="";

        for(Map.Entry<Integer, Integer> entry: foodIdToQuantityMap.entrySet()) {
            FoodItem foodItem = foodIdtoItemMap.get(entry.getKey());
            fcId = foodItem.getFoodCourtId();
            fcName = foodItem.getFoodCourtName();
            totalCost += foodItem.getCost();
            if(foodItem.getTimeToPrepareInMinutes() > maxTime) {
                maxTime = foodItem.getTimeToPrepareInMinutes();
            }
        }
        CustomerOrder customerOrder = new CustomerOrder(0, user.getId() , user.getFirstName() + " " + user.getLastName(), totalCost,
        new Date(),fcId, fcName, ORDER_STATUS_ENUM.ACTIVE, maxTime);
        int orderId = orderDao.createCustomerOrder(customerOrder);
        customerOrder.setId(orderId);



        List<OrderItem> orderItems = new ArrayList<>();
        Set<Integer> restaurantIds = new HashSet<>();
        for(Map.Entry<Integer, Integer> entry: foodIdToQuantityMap.entrySet()) {
            FoodItem foodItem = foodIdtoItemMap.get(entry.getKey());
            Integer restaurantId = foodItem.getRestaurantId();
            OrderItem orderItem = new OrderItem(entry.getKey(), foodItem.getName(), orderId,restaurantId,foodItem.getRestaurantName(), foodItem.getCost(), entry.getValue());
            orderItems.add(orderItem);
            restaurantIds.add(restaurantId);
        }
        orderDao.createOrderItems(orderItems);
        orderDao.createChildOrdersForRestaurant(orderId, restaurantIds);
        return customerOrder;
    }

    public LinkedHashMap<String, List<OrderDetail>> getOrdersForParameters(String email, Integer restaurantId) {
        return segregateOrderBasedOnOrderStatus(orderDao.getOrdersForParameters(email, restaurantId));
    }

    private LinkedHashMap<String, List<OrderDetail>> segregateOrderBasedOnOrderStatus(List<OrderDetail> ordersDetails) {
        LinkedHashMap<String, List<OrderDetail>> resultMap= new LinkedHashMap<>();
        resultMap.put("ACTIVE", new ArrayList<>());
        resultMap.put("COMPLETED", new ArrayList<>());
        for(OrderDetail orderDetail : ordersDetails) {
            if(orderDetail.getCustomerOrder().getOrder_status() == ORDER_STATUS_ENUM.ACTIVE){
                List<OrderDetail> orderDetailList = resultMap.get("ACTIVE");
                orderDetailList.add(orderDetail);
                resultMap.put("ACTIVE", orderDetailList);
            }
            else if (orderDetail.getCustomerOrder().getOrder_status() == ORDER_STATUS_ENUM.COMPLETED) {
                List<OrderDetail> orderDetailList = resultMap.get("COMPLETED");
                orderDetailList.add(orderDetail);
                resultMap.put("COMPLETED", orderDetailList);
            }
        }
        return resultMap;
    }

    public void updateOrderStatus(Integer restaurantId, Integer orderId, ORDER_STATUS_ENUM orderStatus) {
        orderDao.updateOrderStatus(restaurantId ,orderId, orderStatus);
    }

    public Integer getRestaurantIdForUser(String email) throws Exception {
        return userDao.getRestaurantIdForVendor(email);
    }
}
