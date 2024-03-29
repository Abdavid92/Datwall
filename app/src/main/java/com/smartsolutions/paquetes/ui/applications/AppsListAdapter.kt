package com.smartsolutions.paquetes.ui.applications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemAppGroupBinding
import com.smartsolutions.paquetes.databinding.ItemAppBinding
import com.smartsolutions.paquetes.databinding.ItemHeaderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

private const val APP_HOLDER_TYPE = 0
private const val APP_GROUP_HOLDER_TYPE = 1
private const val HEADER_HOLDER_TYPE = 2

class AppsListAdapter constructor(
    private val fragment: Fragment,
    private val launcher: ActivityResultLauncher<App>,
    private val iconManager: IIconManager,
    private var appsFilter: AppsFilter,
    var list: List<IApp>
) : RecyclerView.Adapter<AppsListAdapter.AbstractViewHolder>(),
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var searchJob: Job? = null

    /**
     * Lista filtrada.
     * */
    var finalList: MutableList<IApp> = list.toMutableList()
        private set

    /**
     * Tamaño de los íconos de las aplicaciones.
     * */
    private var iconSize: Int = 50

    /**
     * Mapa que se usa para guardar el estado de los grupos de aplicaciones.
     * Se usa como llave el hashCode del grupo y como valor el estado (true) si está
     * expandido, (false) si está colapsado.
     * */
    private val expandedList = mutableMapOf<Int, Boolean>()

    private val uiHelper = UIHelper(fragment.requireContext())

    /**
     * Retorna el tipo de viewHolder que se debe instanciar.
     * */
    override fun getItemViewType(position: Int): Int {
        return when(finalList[position]) {
            is App -> APP_HOLDER_TYPE
            is AppGroup -> APP_GROUP_HOLDER_TYPE
            else -> HEADER_HOLDER_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AbstractViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        /*
         * Retorno el ViewHolder correspondiente al tipo de app.
         * */
        return when (viewType) {
            APP_HOLDER_TYPE -> {
                AppViewHolder(
                    ItemAppBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            APP_GROUP_HOLDER_TYPE -> {
                AppGroupViewHolder(
                    ItemAppGroupBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            else -> {
                HeaderViewHolder(
                    ItemHeaderBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
        }
    }

    override fun onBindViewHolder(holder: AbstractViewHolder, position: Int) {
        holder.bind(finalList[position])
    }

    override fun onViewRecycled(holder: AbstractViewHolder) {
        super.onViewRecycled(holder)
        holder.close()
    }

    override fun getItemCount() = finalList.size

    /**
     * Filtra la lista usando el nombre de las aplicaciones.
     * Actualiza la lista con los resultados de la búsqueda.
     *
     * @param query - Texto que debe contener el nombre de la aplicación a buscar.
     * */
    fun search(query: String?) {
        searchJob?.cancel()
        searchJob = null

        searchJob = launch {

            val newList = if (query != null && query.isNotBlank()) {
                list.where { it.name.contains(query, true) }.toMutableList()
            } else {
                list.toMutableList()
            }

            val result = DiffUtil.calculateDiff(DiffCallback(finalList, newList, false))

            if (this.isActive) {
                finalList = newList
                expandedList.clear()

                withContext(Dispatchers.Main) {
                    result.dispatchUpdatesTo(this@AppsListAdapter)
                }
            }
        }
    }

    fun updateList(result: DiffUtil.DiffResult, newList: List<IApp>) {
        finalList = newList.toMutableList()
        expandedList.clear()

        result.dispatchUpdatesTo(this)
    }

    /**
     * Actualiza la lista de aplicaciones y refresca el RecyclerView.
     *
     * @param newList - Lista de aplicaciones.
     * */
    fun updateList(newFilter: AppsFilter, newList: List<IApp>) {
        if (newFilter == appsFilter && newList == list)
            return

        launch {
            val result = DiffUtil.calculateDiff(
                DiffCallback(finalList, newList, appsFilter != newFilter),
                true)

            appsFilter = newFilter
            this@AppsListAdapter.list = newList
            finalList = newList.toMutableList()

            withContext(Dispatchers.Main) {
                result.dispatchUpdatesTo(this@AppsListAdapter)
            }
        }
    }

    /**
     * Actualiza el ViewHolder de una app en la lista. Si la app está dentro
     * de un grupo, actualiza el ViewHolder del grupo.
     *
     * @param app
     * */
    fun updateApp(app: IApp) {
        var index = finalList.indexOf(app)

        if (index != -1) {
            finalList[index] = app
            notifyItemChanged(index)
        } else if (app is App) {
            finalList.filterIsInstance<AppGroup>()
                .forEach { appGroup ->

                    index = appGroup.indexOf(app)

                    if (index != -1) {
                        appGroup[index] = app

                        notifyItemChanged(finalList.indexOf(appGroup))
                    }
                }
        }
    }

    /**
     * Propiedad de extensión para guardar el estado (expanded o collapse)
     * de los grupos de aplicaciones.
     * */
    private var AppGroup.expanded: Boolean
        get() = expandedList[this.hashCode()] ?: false
        set(value) {
            expandedList[this.hashCode()] = value
        }

    /**
     * Método de extensión para buscar instancias de [IApp] en una lista mixta.
     * */
    inline fun List<IApp>.where(predicate: (IApp) -> Boolean): List<IApp> {
        val result = mutableListOf<IApp>()

        forEach {
            if (it is AppGroup) {
                result.addAll(it.filter(predicate))
            } else {
                if (predicate(it))
                    result.add(it)
            }
        }
        return result
    }

    /**
     * Procesa una instancia de [HeaderApp].
     * */
    private inner class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : AbstractViewHolder(binding.root) {

        override fun bind(app: IApp) {
            binding.headerText.text = app.name
        }

        override fun close() {

        }
    }

    /**
     * Procesa una instancia de [App].
     * */
    private inner class AppViewHolder(
        private val binding: ItemAppBinding
    ) : AbstractViewHolder(binding.root) {

        private var iconJob: Job? = null

        override fun bind(app: IApp) {
            //Casteo al tipo App. Si no es este tipo se lanza una excepción
            app as App

            binding.app = app

            binding.backgroundLayout.setOnClickListener {

                val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    fragment.requireActivity(),
                    Pair(binding.backgroundLayout, VIEW_NAME_HEADER_LAYOUT),
                    Pair(binding.icon, VIEW_NAME_HEADER_IMAGE),
                    Pair(binding.name, VIEW_NAME_HEADER_NAME),
                    Pair(binding.packageName, VIEW_NAME_HEADER_PACKAGE_NAME)
                )

                launcher.launch(app, activityOptions)
            }

            //Carga el ícono asíncronamente
            iconJob = iconManager.getIcon(app.packageName, app.version, iconSize) {
                binding.icon.setImageBitmap(it)
            }

            uiHelper.setVpnAccessCheckBoxListener(app, binding.vpnAccess) {
                if (fragment is OnAppChangeListener) {
                    fragment.onAppChange(app)
                }
            }

            if (appsFilter == AppsFilter.InternetAccess) {
                binding.launch.visibility = View.GONE
                binding.vpnAccess.visibility = View.VISIBLE
            } else {
                binding.launch.visibility = View.VISIBLE
                binding.vpnAccess.visibility = View.INVISIBLE
            }
        }

        override fun close() {
            iconJob?.cancel()
            iconJob = null
        }
    }

    /**
     * Procesa una instancia de [AppGroup].
     * */
    private inner class AppGroupViewHolder(
        private val binding: ItemAppGroupBinding
    ) : AbstractViewHolder(binding.root) {

        private var iconJob: Job? = null

        /**
         * Adaptador de la lista de aplicaciones anidada.
         * */
        private var childAdapter: AppsListAdapter? = null

        override fun bind(app: IApp) {
            //Casteo al tipo AppGroup. Si no es este tipo se lanza una excepción
            app as AppGroup

            binding.name.text = app.name
            binding.appsCount
                .text = itemView.context.getString(R.string.apps_count, app.size)

            if (appsFilter == AppsFilter.InternetAccess) {
                binding.vpnAccess.visibility = View.VISIBLE
            } else {
                binding.vpnAccess.visibility = View.INVISIBLE
            }

            binding.childLayout.visibility = if (app.expanded) View.VISIBLE else View.GONE

            binding.backgroundLayout.setOnClickListener {
                app.expanded = !app.expanded
                notifyItemChanged(absoluteAdapterPosition)
            }

            childAdapter = AppsListAdapter(
                fragment,
                launcher,
                iconManager,
                appsFilter,
                app
            ).apply {
                iconSize = 40
            }

            binding.child.adapter = childAdapter

            uiHelper.setVpnAccessCheckBoxListener(app, binding.vpnAccess) {
                if (fragment is OnAppChangeListener) {
                    fragment.onAppChange(app)
                    updateApp(app)
                }
            }

            if (app.expanded) {
                binding.arrow.rotation = 180F
            } else {
                binding.arrow.rotation = 0F
            }

            iconJob = iconManager.getIcon(app.packageName, iconSize) {
                binding.icon.setImageBitmap(it)
            }
        }

        override fun close() {
            iconJob?.cancel()
            iconJob = null
        }
    }

    abstract inner class AbstractViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        abstract fun bind(app: IApp)

        abstract fun close()
    }

    private class DiffCallback(
        private val oldList: List<IApp>,
        private val newList: List<IApp>,
        private val changeFilter: Boolean
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition] && !changeFilter
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].accessHashToken() ==
                    newList[newItemPosition].accessHashToken()
        }

    }

    /**
     * Listener para escuchar los cambios de las apps.
     * */
    interface OnAppChangeListener {
        /**
         * Se invoca cuando una app cambia.
         * */
        fun onAppChange(app: IApp)
    }
}