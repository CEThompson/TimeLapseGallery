package com.vwoom.timelapsegallery.data.source.fakes

import com.vwoom.timelapsegallery.data.source.IWeatherLocalDataSource
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherApi
import com.vwoom.timelapsegallery.weather.WeatherResult

class FakeLocalDataSource(
        var forecastJsonString: String = EMPTY_LOCAL_JSON,
        var isToday: Boolean = false) : IWeatherLocalDataSource {

    override suspend fun cacheForecast(forecastResponse: ForecastResponse) {
        val jsonString = WeatherApi.moshi.adapter(ForecastResponse::class.java).toJson(forecastResponse)
        forecastJsonString = jsonString
    }

    override suspend fun getCachedWeather(): WeatherResult<ForecastResponse> {
        return try {
            val forecastFromStorage: ForecastResponse? = WeatherApi.moshi.adapter(ForecastResponse::class.java)
                    .fromJson(forecastJsonString)
            println("$forecastFromStorage")
            when {
                forecastFromStorage == null -> {
                    println("forecast from storage is null")
                    WeatherResult.NoData()
                }
                !isToday -> {
                    println("is not today")
                    WeatherResult.CachedForecast(
                            forecastFromStorage,
                            System.currentTimeMillis(),
                            null
                    )
                }
                else -> {
                    println("is today")
                    WeatherResult.TodaysForecast(forecastFromStorage, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            println("exception: $e")
            WeatherResult.NoData()
        }
    }

    companion object {
        const val EMPTY_LOCAL_JSON = ""

        const val TEST_JSON = "{\n" +
                "    \"@context\": [\n" +
                "        \"https://geojson.org/geojson-ld/geojson-context.jsonld\",\n" +
                "        {\n" +
                "            \"@version\": \"1.1\",\n" +
                "            \"wx\": \"https://api.weather.gov/ontology#\",\n" +
                "            \"geo\": \"http://www.opengis.net/ont/geosparql#\",\n" +
                "            \"unit\": \"http://codes.wmo.int/common/unit/\",\n" +
                "            \"@vocab\": \"https://api.weather.gov/ontology#\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"type\": \"Feature\",\n" +
                "    \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "            [\n" +
                "                [\n" +
                "                    -77.036997299999996,\n" +
                "                    38.900782700000001\n" +
                "                ],\n" +
                "                [\n" +
                "                    -77.040754800000002,\n" +
                "                    38.878836499999998\n" +
                "                ],\n" +
                "                [\n" +
                "                    -77.012551900000005,\n" +
                "                    38.875908599999995\n" +
                "                ],\n" +
                "                [\n" +
                "                    -77.008788700000011,\n" +
                "                    38.897854499999994\n" +
                "                ],\n" +
                "                [\n" +
                "                    -77.036997299999996,\n" +
                "                    38.900782700000001\n" +
                "                ]\n" +
                "            ]\n" +
                "        ]\n" +
                "    },\n" +
                "    \"properties\": {\n" +
                "        \"updated\": \"2020-07-31T20:33:25+00:00\",\n" +
                "        \"units\": \"us\",\n" +
                "        \"forecastGenerator\": \"BaselineForecastGenerator\",\n" +
                "        \"generatedAt\": \"2020-07-31T21:31:37+00:00\",\n" +
                "        \"updateTime\": \"2020-07-31T20:33:25+00:00\",\n" +
                "        \"validTimes\": \"2020-07-31T14:00:00+00:00/P7DT11H\",\n" +
                "        \"elevation\": {\n" +
                "            \"value\": 6.0960000000000001,\n" +
                "            \"unitCode\": \"unit:m\"\n" +
                "        },\n" +
                "        \"periods\": [\n" +
                "            {\n" +
                "                \"number\": 1,\n" +
                "                \"name\": \"This Afternoon\",\n" +
                "                \"startTime\": \"2020-07-31T17:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-07-31T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 84,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"3 mph\",\n" +
                "                \"windDirection\": \"NE\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra,60?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms Likely\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms likely. Mostly cloudy, with a high near 84. Northeast wind around 3 mph. Chance of precipitation is 60%. New rainfall amounts between a tenth and quarter of an inch possible.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 2,\n" +
                "                \"name\": \"Tonight\",\n" +
                "                \"startTime\": \"2020-07-31T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-01T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 71,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"5 mph\",\n" +
                "                \"windDirection\": \"NE\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra,60/tsra,20?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms Likely\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms likely before 3am. Mostly cloudy, with a low around 71. Northeast wind around 5 mph. Chance of precipitation is 60%. New rainfall amounts between a quarter and half of an inch possible.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 3,\n" +
                "                \"name\": \"Saturday\",\n" +
                "                \"startTime\": \"2020-08-01T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-01T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 88,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"3 to 7 mph\",\n" +
                "                \"windDirection\": \"E\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/bkn,20/tsra,30?size=medium\",\n" +
                "                \"shortForecast\": \"Mostly Cloudy then Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A chance of showers and thunderstorms after noon. Mostly cloudy, with a high near 88. East wind 3 to 7 mph. Chance of precipitation is 30%. New rainfall amounts less than a tenth of an inch possible.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 4,\n" +
                "                \"name\": \"Saturday Night\",\n" +
                "                \"startTime\": \"2020-08-01T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-02T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 75,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"7 mph\",\n" +
                "                \"windDirection\": \"SE\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra,60/tsra,50?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms Likely\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms likely. Mostly cloudy, with a low around 75. Southeast wind around 7 mph. Chance of precipitation is 60%. New rainfall amounts between 1 and 2 inches possible.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 5,\n" +
                "                \"name\": \"Sunday\",\n" +
                "                \"startTime\": \"2020-08-02T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-02T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 94,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"6 to 13 mph\",\n" +
                "                \"windDirection\": \"S\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra_hi,40/tsra_hi,30?size=medium\",\n" +
                "                \"shortForecast\": \"Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A chance of showers and thunderstorms before noon, then a chance of showers and thunderstorms. Mostly sunny, with a high near 94. South wind 6 to 13 mph, with gusts as high as 22 mph. Chance of precipitation is 40%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 6,\n" +
                "                \"name\": \"Sunday Night\",\n" +
                "                \"startTime\": \"2020-08-02T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-03T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 75,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"6 to 12 mph\",\n" +
                "                \"windDirection\": \"SW\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra_sct,40?size=medium\",\n" +
                "                \"shortForecast\": \"Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A chance of showers and thunderstorms before 8pm, then a chance of showers and thunderstorms. Mostly cloudy, with a low around 75. Chance of precipitation is 40%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 7,\n" +
                "                \"name\": \"Monday\",\n" +
                "                \"startTime\": \"2020-08-03T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-03T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 85,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"6 mph\",\n" +
                "                \"windDirection\": \"S\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra,40/tsra,80?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms. Mostly cloudy, with a high near 85. Chance of precipitation is 80%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 8,\n" +
                "                \"name\": \"Monday Night\",\n" +
                "                \"startTime\": \"2020-08-03T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-04T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 72,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"6 mph\",\n" +
                "                \"windDirection\": \"S\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra,80?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms. Mostly cloudy, with a low around 72. Chance of precipitation is 80%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 9,\n" +
                "                \"name\": \"Tuesday\",\n" +
                "                \"startTime\": \"2020-08-04T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-04T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 86,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"5 mph\",\n" +
                "                \"windDirection\": \"SW\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra_sct,80/tsra_sct,70?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms. Partly sunny, with a high near 86. Chance of precipitation is 80%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 10,\n" +
                "                \"name\": \"Tuesday Night\",\n" +
                "                \"startTime\": \"2020-08-04T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-05T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 71,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"1 to 5 mph\",\n" +
                "                \"windDirection\": \"SW\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra_hi,70/tsra_hi,40?size=medium\",\n" +
                "                \"shortForecast\": \"Showers And Thunderstorms Likely then Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"Showers and thunderstorms likely before 8pm, then a chance of showers and thunderstorms between 8pm and 2am, then a chance of showers and thunderstorms. Mostly cloudy, with a low around 71. Chance of precipitation is 70%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 11,\n" +
                "                \"name\": \"Wednesday\",\n" +
                "                \"startTime\": \"2020-08-05T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-05T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 87,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"3 mph\",\n" +
                "                \"windDirection\": \"W\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra_hi,50?size=medium\",\n" +
                "                \"shortForecast\": \"Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A chance of showers and thunderstorms. Partly sunny, with a high near 87. Chance of precipitation is 50%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 12,\n" +
                "                \"name\": \"Wednesday Night\",\n" +
                "                \"startTime\": \"2020-08-05T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-06T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 69,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"3 mph\",\n" +
                "                \"windDirection\": \"W\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra_hi,50/tsra_hi?size=medium\",\n" +
                "                \"shortForecast\": \"Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A chance of showers and thunderstorms. Mostly cloudy, with a low around 69. Chance of precipitation is 50%.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 13,\n" +
                "                \"name\": \"Thursday\",\n" +
                "                \"startTime\": \"2020-08-06T06:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-06T18:00:00-04:00\",\n" +
                "                \"isDaytime\": true,\n" +
                "                \"temperature\": 87,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"3 mph\",\n" +
                "                \"windDirection\": \"NW\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/day/tsra_hi?size=medium\",\n" +
                "                \"shortForecast\": \"Slight Chance Showers And Thunderstorms\",\n" +
                "                \"detailedForecast\": \"A slight chance of rain showers before 8am, then a slight chance of showers and thunderstorms. Mostly sunny, with a high near 87.\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"number\": 14,\n" +
                "                \"name\": \"Thursday Night\",\n" +
                "                \"startTime\": \"2020-08-06T18:00:00-04:00\",\n" +
                "                \"endTime\": \"2020-08-07T06:00:00-04:00\",\n" +
                "                \"isDaytime\": false,\n" +
                "                \"temperature\": 68,\n" +
                "                \"temperatureUnit\": \"F\",\n" +
                "                \"temperatureTrend\": null,\n" +
                "                \"windSpeed\": \"2 mph\",\n" +
                "                \"windDirection\": \"S\",\n" +
                "                \"icon\": \"https://api.weather.gov/icons/land/night/tsra_hi/sct?size=medium\",\n" +
                "                \"shortForecast\": \"Slight Chance Showers And Thunderstorms then Partly Cloudy\",\n" +
                "                \"detailedForecast\": \"A slight chance of showers and thunderstorms before 8pm. Partly cloudy, with a low around 68.\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }\n" +
                "}"
    }
}