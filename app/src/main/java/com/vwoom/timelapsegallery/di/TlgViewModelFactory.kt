package com.vwoom.timelapsegallery.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.IllegalArgumentException
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class TlgViewModelFactory @Inject constructor(
        private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val creator = creators[modelClass] ?: creators.entries.firstOrNull(){
            modelClass.isAssignableFrom(it.key)
        }        ?.value ?: throw IllegalArgumentException("unknown model class")
        return creator.get() as T
    }

}

// TODO delete alternate versions

/*class ViewModelFactory<T : ViewModel> @Inject constructor(
        private val viewModel: Lazy<T>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = viewModel.get() as T
}*/


/*
class ViewModelFactory @Inject constructor(
        private val providers: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = requireNotNull(getProvider(modelClass).get()){
        "Provider for $modelClass returned null"
    }

    @Suppress("unchecked_cast")
    private fun <T: ViewModel> getProvider(modelClass: Class<T>): Provider<T> =
            try {
                requireNotNull(providers[modelClass] as Provider<T>) {
                    "No ViewModel provider is bound for class $modelClass"
                }
            } catch (cce: ClassCastException){
                error("Wrong provider type registered for ViewModel type $modelClass")
            }
}
*/
