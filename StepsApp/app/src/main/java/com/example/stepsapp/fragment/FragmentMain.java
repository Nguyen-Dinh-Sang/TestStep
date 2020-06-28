package com.example.stepsapp.fragment;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.stepsapp.R;
import com.example.stepsapp.data.DataBaseManager;

import org.eazegraph.lib.charts.PieChart;
import org.eazegraph.lib.models.PieModel;

public class FragmentMain extends Fragment implements SensorEventListener {
    private TextView textViewsteps, textViewtotal, textViewaverage;
    private PieModel sliceGoal, sliceCurrent;
    private PieChart pieChart;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container,false);
        initView(view);
        Toast.makeText(getContext(), DataBaseManager.getInstance(getContext()).getDays() + "",Toast.LENGTH_LONG).show();
        return view;
    }

    private void initView(View view) {
        textViewsteps = view.findViewById(R.id.tv_steps);
        textViewaverage = view.findViewById(R.id.tv_average);
        textViewtotal = view.findViewById(R.id.tv_total);
        pieChart = view.findViewById(R.id.pc_graph);

//        sliceCurrent = new PieModel("", 100, Color.parseColor("#FF00B3"));
//        pieChart.addPieSlice(sliceCurrent);
        sliceGoal = new PieModel("", 10000, Color.parseColor("#00FFE1"));
        pieChart.addPieSlice(sliceGoal);

        pieChart.setDrawValueInPie(false);
        pieChart.setUsePieRotation(true);
        pieChart.startAnimation();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
            return;
        }
        textViewsteps.setText(event.values[0] + "");
        sliceCurrent = new PieModel("", event.values[0], Color.parseColor("#FF00B3"));
        pieChart.addPieSlice(sliceCurrent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onResume() {
        super.onResume();
        SensorManager sm = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
    }
}
