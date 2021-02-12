package com.vwoom.timelapsegallery.gallery

import android.location.Location
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vwoom.timelapsegallery.data.entry.ProjectTagEntry
import com.vwoom.timelapsegallery.data.entry.TagEntry
import com.vwoom.timelapsegallery.data.repository.IProjectRepository
import com.vwoom.timelapsegallery.data.repository.ITagRepository
import com.vwoom.timelapsegallery.data.repository.IWeatherRepository
import com.vwoom.timelapsegallery.data.view.ProjectView
import com.vwoom.timelapsegallery.utils.TimeUtils.daysUntilDue
import com.vwoom.timelapsegallery.weather.data.ForecastResponse
import com.vwoom.timelapsegallery.weather.WeatherResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class SearchType {
    None, DueToday, DueTomorrow, Pending, Scheduled, Unscheduled
}

class GalleryViewModel
@Inject constructor(projectRepository: IProjectRepository,
                    private val tagRepository: ITagRepository,
                    private val weatherRepository: IWeatherRepository) : ViewModel() {
    // For observing all projects
    val projects: LiveData<List<ProjectView>> = projectRepository.getProjectViewsLiveData()

    // For the displayed projects in search filtration
    val displayedProjectViews = MutableLiveData(emptyList<ProjectView>())

    // Tags for all projects to populate the search dialog
    val tags: LiveData<List<TagEntry>> = tagRepository.getTagsLiveData()

    // Weather forecast
    val weather = MutableLiveData<WeatherResult<ForecastResponse>>(WeatherResult.Loading)

    // Search state: weather a search is active or not
    val search = MutableLiveData(false)


    // Inputted search data
    var searchTags: ArrayList<TagEntry> = arrayListOf()
    var searchName: String = ""
    var searchType = SearchType.None

    // State of showing dialogs
    var searchDialogShowing = false
    var weatherChartDialogShowing = false
    var weatherDetailsDialogShowing = false

    // For use when search launched from notification
    // To prevent search activating when returning to the gallery, after the user decided to stop the search
    var userClickedToStopSearch = false

    private var searchJob: Job = Job()

    override fun onCleared() {
        super.onCleared()
        searchJob.cancel()
    }

    private fun isUserSearching(): Boolean {
        return searchTags.isNotEmpty()
                || searchType != SearchType.None
                || searchName.isNotBlank()
    }

    // Returns whether or not a tag was selected by the user
    fun tagSelected(tag: TagEntry): Boolean {
        return searchTags.contains(tag)
    }

    // TODO: simplify and clarify forecast retrieval
    // Retrieves cached forecast and attempts to update it if forecast does not belong to today
    fun getForecast(location: Location?) {
        weather.value = WeatherResult.Loading
        viewModelScope.launch {
            // Get the cached forecast
            weather.value = weatherRepository.getCachedForecast()

            // If it doesn't belong to today then try to update
            if (weather.value !is WeatherResult.TodaysForecast && location != null) {
                updateForecast(location)
            }
        }
    }

    // Attempts to update the forecast
    fun updateForecast(location: Location) {
        if (weather.value != WeatherResult.Loading) weather.value = WeatherResult.Loading
        viewModelScope.launch {
            weather.value = weatherRepository.updateForecast(location)
        }
    }

    fun setForecastNoData() {
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
                SearchType.Scheduled -> {
                    resultProjects = resultProjects.filter {
                        return@filter it.interval_days > 0
                    }
                }
                SearchType.Unscheduled -> {
                    resultProjects = resultProjects.filter {
                        return@filter it.interval_days == 0
                    }
                }
                SearchType.DueToday -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) <= 0
                    }
                }
                SearchType.DueTomorrow -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) == 1.toLong()
                    }
                }
                SearchType.Pending -> {
                    resultProjects = resultProjects.filter {
                        if (it.interval_days == 0) return@filter false
                        return@filter daysUntilDue(it) > 0
                    }
                    resultProjects = resultProjects.sortedBy { daysUntilDue(it) } // show projects due earlier first
                }
                else -> {
                    // Do nothing
                }
            }
            displayedProjectViews.value = resultProjects
        }
    }

    fun setSearch() {
        search.value = isUserSearching()
    }

    fun clearSearch() {
        searchName = ""
        searchTags.clear()
        searchType = SearchType.None
        filterProjects()
        setSearch()
    }

}