package com.example.shawmori.gyroscopetest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class DataActivity extends AppCompatActivity {

    //Data storage
    private Coordinate[] coordinates;
    private int size;

    //Statistics
    private float xAv, yAv, zAv, xMin, xMax, yMin, yMax, zMin, zMax;

    //UI Elements
    TextView averages, minMax;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        averages = (TextView) findViewById(R.id.averages);
        minMax = (TextView) findViewById(R.id.minMax);

        Intent i = getIntent();
        coordinates = (Coordinate[])i.getSerializableExtra("data");
        size = coordinates.length;

        getAverage();
        getMinMax();
    }

    /**
     * Sets the averages TextView to the averages of the coordinates
     */
    public void getAverage(){
        float xTotal = 0;
        float yTotal = 0;
        float zTotal = 0;

        for(int i = 0; i < size; i++) {
            xTotal+=coordinates[i].getX();
            yTotal+=coordinates[i].getY();
            zTotal+=coordinates[i].getZ();
        }

        xAv = xTotal / size;
        yAv = yTotal / size;
        zAv = zTotal / size;

        averages.setText("Averages:\n" +
                "X Average: " + xAv + "\nY Average: " + yAv + "\nZ Average: " + zAv);
    }

    /**
     * Sets the minMax TextViews to the min and max of the data.
     */
    public void getMinMax(){
        xMin = Float.MAX_VALUE;
        yMin = Float.MAX_VALUE;
        zMin = Float.MAX_VALUE;

        xMax = Float.MIN_VALUE;
        yMax = Float.MIN_VALUE;
        zMax = Float.MIN_VALUE;

        for(int i = 0; i < size; i++) {
            if(coordinates[i].getX() < xMin)
                xMin = coordinates[i].getX();
            if(coordinates[i].getY() < yMin)
                yMin = coordinates[i].getY();
            if(coordinates[i].getZ() < zMin)
                zMin = coordinates[i].getZ();
            if(coordinates[i].getX() > xMax)
                xMax = coordinates[i].getX();
            if(coordinates[i].getY() > yMax)
                yMax = coordinates[i].getY();
            if(coordinates[i].getX() > zMax)
                zMax = coordinates[i].getZ();
        }

        minMax.setText("Mins & Maxs\nX Minimum: " + xMin + "\nX Maximum: " + xMax + "\nY Minimum: " + yMin + "\nY Maximum: " + yMax + "\nZ Minimum: " + zMin + "\nZ Maximum: " + zMax);
    }
}
