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

package com.lengkenglab.wifianalyzer.wifi.channelgraph;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.view.View;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.TitleLineGraphSeries;
import com.lengkenglab.wifianalyzer.Configuration;
import com.lengkenglab.wifianalyzer.MainActivity;
import com.lengkenglab.wifianalyzer.MainContext;
import com.lengkenglab.wifianalyzer.R;
import com.lengkenglab.wifianalyzer.settings.Settings;
import com.lengkenglab.wifianalyzer.wifi.band.WiFiBand;
import com.lengkenglab.wifianalyzer.wifi.band.WiFiChannel;
import com.lengkenglab.wifianalyzer.wifi.band.WiFiChannels;
import com.lengkenglab.wifianalyzer.wifi.graphutils.GraphColor;
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

class ChannelGraphView implements GraphViewNotifier, GraphConstants {
    private final WiFiBand wiFiBand;
    private final Pair<WiFiChannel, WiFiChannel> wiFiChannelPair;
    private GraphViewWrapper graphViewWrapper;
    private DataManager dataManager;

    ChannelGraphView(@NonNull WiFiBand wiFiBand, @NonNull Pair<WiFiChannel, WiFiChannel> wiFiChannelPair) {
        this.wiFiBand = wiFiBand;
        this.wiFiChannelPair = wiFiChannelPair;
        this.graphViewWrapper = makeGraphViewWrapper();
        this.dataManager = new DataManager();
    }

    @Override
    public void update(@NonNull WiFiData wiFiData) {
        Settings settings = MainContext.INSTANCE.getSettings();
        Predicate<WiFiDetail> predicate = FilterPredicate.makeOtherPredicate(settings);
        List<WiFiDetail> wiFiDetails = wiFiData.getWiFiDetails(predicate, settings.getSortBy());
        Set<WiFiDetail> newSeries = dataManager.getNewSeries(wiFiDetails, wiFiChannelPair);
        dataManager.addSeriesData(graphViewWrapper, newSeries, settings.getGraphMaximumY());
        graphViewWrapper.removeSeries(newSeries);
        graphViewWrapper.updateLegend(settings.getChannelGraphLegend());
        graphViewWrapper.setVisibility(isSelected() ? View.VISIBLE : View.GONE);
    }

    private boolean isSelected() {
        Settings settings = MainContext.INSTANCE.getSettings();
        WiFiBand wiFiBand = settings.getWiFiBand();
        Configuration configuration = MainContext.INSTANCE.getConfiguration();
        Pair<WiFiChannel, WiFiChannel> wiFiChannelPair = configuration.getWiFiChannelPair();
        return this.wiFiBand.equals(wiFiBand) && (WiFiBand.GHZ2.equals(this.wiFiBand) || this.wiFiChannelPair.equals(wiFiChannelPair));
    }

    @Override
    public GraphView getGraphView() {
        return graphViewWrapper.getGraphView();
    }

    private int getNumX() {
        int channelFirst = wiFiChannelPair.first.getChannel() - WiFiChannels.CHANNEL_OFFSET;
        int channelLast = wiFiChannelPair.second.getChannel() + WiFiChannels.CHANNEL_OFFSET;
        return Math.min(NUM_X_CHANNEL, channelLast - channelFirst + 1);
    }

    private GraphView makeGraphView(@NonNull MainActivity mainActivity, int graphMaximumY) {
        Resources resources = mainActivity.getResources();
        return new GraphViewBuilder(mainActivity, getNumX(), graphMaximumY)
            .setLabelFormatter(new ChannelAxisLabel(wiFiBand, wiFiChannelPair))
            .setVerticalTitle(resources.getString(R.string.graph_axis_y))
            .setHorizontalTitle(resources.getString(R.string.graph_channel_axis_x))
            .build();
    }

    private GraphViewWrapper makeGraphViewWrapper() {
        MainContext mainContext = MainContext.INSTANCE;
        MainActivity mainActivity = mainContext.getMainActivity();
        Settings settings = mainContext.getSettings();
        Configuration configuration = mainContext.getConfiguration();
        GraphView graphView = makeGraphView(mainActivity, settings.getGraphMaximumY());
        graphViewWrapper = new GraphViewWrapper(graphView, settings.getChannelGraphLegend());
        configuration.setSize(graphViewWrapper.getSize(graphViewWrapper.calculateGraphType()));
        int minX = wiFiChannelPair.first.getFrequency() - WiFiChannels.FREQUENCY_OFFSET;
        int maxX = minX + (graphViewWrapper.getViewportCntX() * WiFiChannels.FREQUENCY_SPREAD);
        graphViewWrapper.setViewport(minX, maxX);
        graphViewWrapper.addSeries(makeDefaultSeries(wiFiChannelPair.second.getFrequency(), minX));
        return graphViewWrapper;
    }

    private TitleLineGraphSeries<DataPoint> makeDefaultSeries(int frequencyEnd, int minX) {
        DataPoint[] dataPoints = new DataPoint[]{
            new DataPoint(minX, MIN_Y),
            new DataPoint(frequencyEnd + WiFiChannels.FREQUENCY_OFFSET, MIN_Y)
        };

        TitleLineGraphSeries<DataPoint> series = new TitleLineGraphSeries<>(dataPoints);
        series.setColor((int) GraphColor.TRANSPARENT.getPrimary());
        series.setThickness(THICKNESS_INVISIBLE);
        return series;
    }

    void setGraphViewWrapper(@NonNull GraphViewWrapper graphViewWrapper) {
        this.graphViewWrapper = graphViewWrapper;
    }

    void setDataManager(@NonNull DataManager dataManager) {
        this.dataManager = dataManager;
    }

}
