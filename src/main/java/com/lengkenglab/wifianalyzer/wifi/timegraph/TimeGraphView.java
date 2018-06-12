/*
 * WiFiAnalyzer
 * Copyright (C) 2017  VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.lengkenglab.wifianalyzer.wifi.timegraph;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.lengkenglab.wifianalyzer.Configuration;
import com.lengkenglab.wifianalyzer.MainActivity;
import com.lengkenglab.wifianalyzer.MainContext;
import com.lengkenglab.wifianalyzer.R;
import com.lengkenglab.wifianalyzer.settings.Settings;
import com.lengkenglab.wifianalyzer.wifi.band.WiFiBand;
import com.lengkenglab.wifianalyzer.wifi.graphutils.GraphConstants;
import com.lengkenglab.wifianalyzer.wifi.graphutils.GraphViewBuilder;
import com.lengkenglab.wifianalyzer.wifi.graphutils.GraphViewNotifier;
import com.lengkenglab.wifianalyzer.wifi.graphutils.GraphViewWrapper;
import com.lengkenglab.wifianalyzer.wifi.model.WiFiData;
import com.lengkenglab.wifianalyzer.wifi.model.WiFiDetail;
import com.lengkenglab.wifianalyzer.wifi.predicate.FilterPredicate;

import org.apache.commons.collections4.Predicate;

import java.util.List;
import java.util.Set;

class TimeGraphView implements GraphViewNotifier, GraphConstants {
    private final WiFiBand wiFiBand;
    private DataManager dataManager;
    private GraphViewWrapper graphViewWrapper;

    TimeGraphView(@NonNull WiFiBand wiFiBand) {
        this.wiFiBand = wiFiBand;
        this.graphViewWrapper = makeGraphViewWrapper();
        this.dataManager = new DataManager();
    }

    @Override
    public void update(@NonNull WiFiData wiFiData) {
        Settings settings = MainContext.INSTANCE.getSettings();
        Predicate<WiFiDetail> predicate = FilterPredicate.makeOtherPredicate(settings);
        List<WiFiDetail> wiFiDetails = wiFiData.getWiFiDetails(predicate, settings.getSortBy());
        Set<WiFiDetail> newSeries = dataManager.addSeriesData(graphViewWrapper, wiFiDetails, settings.getGraphMaximumY());
        graphViewWrapper.removeSeries(newSeries);
        graphViewWrapper.updateLegend(settings.getTimeGraphLegend());
        graphViewWrapper.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
    }

    private boolean isSelected() {
        return wiFiBand.equals(MainContext.INSTANCE.getSettings().getWiFiBand());
    }

    @Override
    public GraphView getGraphView() {
        return graphViewWrapper.getGraphView();
    }

    private int getNumX() {
        return NUM_X_TIME;
    }

    void setGraphViewWrapper(@NonNull GraphViewWrapper graphViewWrapper) {
        this.graphViewWrapper = graphViewWrapper;
    }

    void setDataManager(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

    private GraphView makeGraphView(@NonNull MainActivity mainActivity, int graphMaximumY) {
        Resources resources = mainActivity.getResources();
        return new GraphViewBuilder(mainActivity, getNumX(), graphMaximumY)
            .setLabelFormatter(new TimeAxisLabel())
            .setVerticalTitle(resources.getString(R.string.graph_axis_y))
            .setHorizontalTitle(resources.getString(R.string.graph_time_axis_x))
            .setHorizontalLabelsVisible(false)
            .build();
    }

    private GraphViewWrapper makeGraphViewWrapper() {
        MainContext mainContext = MainContext.INSTANCE;
        MainActivity mainActivity = mainContext.getMainActivity();
        Settings settings = mainContext.getSettings();
        Configuration configuration = mainContext.getConfiguration();
        GraphView graphView = makeGraphView(mainActivity, settings.getGraphMaximumY());
        graphViewWrapper = new GraphViewWrapper(graphView, settings.getTimeGraphLegend());
        configuration.setSize(graphViewWrapper.getSize(graphViewWrapper.calculateGraphType()));
        graphViewWrapper.setViewport();
        return graphViewWrapper;
    }

}
