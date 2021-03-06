package com.windsor.foodapp.service;

import com.windsor.foodapp.dao.RestaurantDao;
import com.windsor.foodapp.model.Restaurant;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


@Component
public class RestaurantService {

    @Resource
    RestaurantDao restaurantDao;

    public List<Restaurant> getRestaurantByFoodCourt(int fc_id)  throws Exception{
        return restaurantDao.getRestaurantByFoodCourt(fc_id);
    }
}
