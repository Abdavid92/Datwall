package com.smartsolutions.paquetes.ui.applications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemAppGroupBinding
import com.smartsolutions.paquetes.databinding.ItemAppBinding
import com.smartsolutions.paquetes.databinding.ItemHeaderBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.Job
import kotlinx.parcelize.Parcelize

private const val APP_HOLDER_TYPE = 0
private const val APP_GROUP_HOLDER_TYPE = 1
private const val HEADER_HOLDER_TYPE = 2

class AppsListAdapter constructor(
    private val fragment: Fragment,
    private val launcher: ActivityResultLauncher<App>,
    private val iconManager: IIconManager,
    private var appsFilter: AppsFilter
) : ListAdapter<IApp, AppsListAdapter.AbstractViewHolder>(DiffCallback) {

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
        return when(getItem(position)) {
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
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: AbstractViewHolder) {
        super.onViewRecycled(holder)
        holder.close()
    }

    /**
     * Muestra [rows] (apps, grupos y cabeceras). Si cambió el filtro se redibujan todas las
     * filas, porque el filtro decide qué controles se ven.
     * */
    fun submit(filter: AppsFilter, rows: List<IApp>) {
        val filterChanged = filter != appsFilter
        appsFilter = filter

        submitList(rows) {
            if (filterChanged)
                notifyItemRangeChanged(0, itemCount)
        }
    }

    /**
     * Actualiza el ViewHolder de una app en la lista. Si la app está dentro
     * de un grupo, actualiza el ViewHolder del grupo.
     *
     * @param app
     * */
    fun updateApp(app: IApp) {
        val index = currentList.indexOf(app)

        if (index != -1) {
            submitList(currentList.toMutableList().also { it[index] = app })
        } else if (app is App) {
            currentList.filterIsInstance<AppGroup>()
                .forEach { appGroup ->

                    val appIndex = appGroup.indexOf(app)

                    if (appIndex != -1) {
                        appGroup[appIndex] = app

                        notifyItemChanged(currentList.indexOf(appGroup))
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

            binding.name.text = app.name
            binding.packageName.text = app.packageName

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

            binding.vpnAccess.bindVpnAccess(app) {
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
                appsFilter
            ).apply {
                iconSize = 40
                submitList(app.toList())
            }

            binding.child.adapter = childAdapter

            binding.vpnAccess.bindVpnAccess(app) {
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

    private object DiffCallback : DiffUtil.ItemCallback<IApp>() {

        override fun areItemsTheSame(oldItem: IApp, newItem: IApp): Boolean =
            if (oldItem is HeaderApp || newItem is HeaderApp)
                oldItem is HeaderApp && newItem is HeaderApp && oldItem.name == newItem.name
            else
                oldItem == newItem

        override fun areContentsTheSame(oldItem: IApp, newItem: IApp): Boolean =
            oldItem.accessHashToken() == newItem.accessHashToken()
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

/**
 * Modelo que se usa para dibujar un encabezado en la lista. Solo
 * contiene el nombre del encabezado en la propiedad name. Las demas
 * propiedades están vacias o nulas. El método [accessHashToken] no está
 * soportado.
 * */
@Parcelize
class HeaderApp private constructor(
    override var packageName: String,
    override var uid: Int,
    override var name: String,
    override var access: Boolean,
    override var system: Boolean,
    override var allowAnnotations: String?,
    override var blockedAnnotations: String?
) : IApp {

    override fun accessHashToken(): String {
        return "0"
    }

    companion object {
        fun newInstance(headerText: String): HeaderApp {
            return HeaderApp(
                "",
                -1,
                headerText,
                access = false,
                system = false,
                null,
                null
            )
        }
    }
}
