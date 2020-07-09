package com.vwoom.timelapsegallery.gallery

import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.ProjectRepository
import com.vwoom.timelapsegallery.data.repository.TagRepository
import com.vwoom.timelapsegallery.data.repository.WeatherRepository
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import com.vwoom.timelapsegallery.weather.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

const val SEARCH_TYPE_NONE = "none"
const val SEARCH_TYPE_DUE_TODAY = "due_today"
const val SEARCH_TYPE_DUE_TOMORROW = "due_tomorrow"
const val SEARCH_TYPE_PENDING = "pending"
const val SEARCH_TYPE_SCHEDULED = "scheduled"
const val SEARCH_TYPE_UNSCHEDULED = "unscheduled"

class GalleryViewModel
@Inject constructor(projectRepository: ProjectRepository,
                    private val tagRepository: TagRepository,
                    private val weatherRepository: WeatherRepository) : ViewModel() {
    // For observing all projects
    val projects: LiveData<List<ProjectView>> = projectRepository.getProjectViewsLiveData()

    // TODO: reconsider these getter patterns. Is this really necessary?
    // For the displayed projects in search filtration
    val displayedProjectViews = MutableLiveData(emptyList<ProjectView>())
    /*val displayedProjectViews: LiveData<List<ProjectView>>
        get() = _displayedProjectViews*/

    // Tags for all projects to populate the search dialog
    val tags: LiveData<List<TagEntry>> = tagRepository.getTagsLiveData()

    // Weather forecast
    val weather = MutableLiveData<WeatherResult<ForecastResponse>>(WeatherResult.Loading)
    /*val weather: LiveData<WeatherResult<ForecastResponse>>
        get() = _weather*/

    // Search state: weather a search is active or not
    val search = MutableLiveData(false)
    /*val search: LiveData<Boolean>
        get() = _search*/

    // Inputted search data
    var searchTags: ArrayList<TagEntry> = arrayListOf()
    var searchName: String = ""
    var searchType: String = SEARCH_TYPE_NONE

    // State of showing dialogs
    var searchDialogShowing = false
    var weatherChartDialogShowing = false
    var weatherDetailsDialogShowing = false

    // For use when search launched from notification
    // To prevent search activating when returning to the gallery, after the user decided to stop the search
    var userClickedToStopSearch = false

    private var searchJob: Job = Job()

    init {
        viewModelScope.launch {
            weather.value = weatherRepository.getForecast()
        }

    }

    override fun onCleared() {
        super.onCleared()
        searchJob.cancel()
    }

    private fun userIsSearching(): Boolean {
        return searchTags.isNotEmpty()
                || searchType != SEARCH_TYPE_NONE
                || !searchName.isBlank()
    }

    // Returns whether or not a tag was selected by the user
    fun tagSelected(tag: TagEntry): Boolean {
        return searchTags.contains(tag)
    }

    // Retrieves cached forecast and attempts to update it if forecast does not belong to today
    fun getForecast(location: Location?) {
        viewModelScope.launch {
            // Get the cached forecast
            //weather.value = weatherRepository.getForecast()

            // If it doesn't belong to today then try to update
            if (weather.value !is WeatherResult.TodaysForecast && location!=null) {
                //Log.d("WeatherDebug", "location is not null, calling update forecast")
                updateForecast(location)
            }
        }
    }

    // Attempts to update the forecast
    fun updateForecast(location: Location) {
        viewModelScope.launch {
            weather.value = weatherRepository.updateForecast(location)
            //Log.d("WeatherDebug", "called update forecast")
        }
    }

    fun setLoading() {
        weather.value = WeatherResult.Loading
    }

    fun forecastDenied() {
        weather.value = WeatherResult.NoData()
    }

    // Filters the projects by the inputted search parameters
    fun filterProjects() {
        searchJob.cancel()
        searchJob = viewModelScope.launch {
            if (projects.value == null) displayedProjectViews.value = listOf()
            var resultProjects = projects.value!!

            if (searchTags.isNotEmpty()) {
                resultProjects = resultProjects.filter {
                    val projectTags: List<ProjectTagEntry> = tagRepository.getProjectTags(it.project_id)
                    val tagEntriesForProject: List<TagEntry> = tagRepository.getTagsFromProjectTags(projectTags)

                    // Include projects with tags included in the search filter
                    for (tag in searchTags)
                        if (tagEntriesForProject.contains(tag)) return@filter true
                    return@filter false
                }
            }

            if (searchName.isNotEmpty()) {
                resultProjects = resultProjects.filter {
                    if (it.project_name == null) return@filter false
                    if (it.project_name.toLowerCase(Locale.getDefault()).contains(searchName.toLowerCase(Locale.getDefault()))) return@filter true
                    return@filter false
                }
            }

            when (searchType) {
                SEARCH_TYPE_SCHEDULED -> {
                    resultProjects = resultProjects.filter {
                        return@filter it.interval_days > 0
                    }
                }
                SEARCH_TYPE_UNSCHEDULED -> {
                    resultProjects = resultProjects.filter {
                        return@filter it.interval_days == 0
                    }
                }
                SEARCH_TYPE_DUE_TODAY -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) <= 0
                    }
                }
                SEARCH_TYPE_DUE_TOMORROW -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) == 1.toLong()
                    }
                }
                SEARCH_TYPE_PENDING -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) > 0
                    }
                    resultProjects = resultProjects.sortedBy { daysUntilDue(it) } // show projects due earlier first
                }
            }
            displayedProjectViews.value = resultProjects
        }
    }

    fun setSearch() {
        search.value = userIsSearching()
    }

    fun clearSearch() {
        searchName = ""
        searchTags.clear()
        searchType = SEARCH_TYPE_NONE
        filterProjects()
        setSearch()
    }

}