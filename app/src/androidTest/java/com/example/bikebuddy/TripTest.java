package com.example.bikebuddy;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TripTest {
    @Test
    public void getDurationInMinutesTest() {
        Trip testTrip = new Trip();
        for(int i = 1; i<6000; i++ ){
            testTrip.duration = i;
            Assert.assertEquals( ((float)(i/600)), testTrip.getDurationInMinutes(), 0.01f);
        }
    }

}