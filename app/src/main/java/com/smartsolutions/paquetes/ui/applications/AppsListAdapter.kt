package com.smartsolutions.paquetes.ui.applications

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.AppGroupItemBinding
import com.smartsolutions.paquetes.databinding.AppItemBinding
import com.smartsolutions.paquetes.databinding.HeaderItemBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp

private const val APP_HOLDER_TYPE = 0
private const val APP_GROUP_HOLDER_TYPE = 1
private const val HEADER_HOLDER_TYPE = 2

class AppsListAdapter constructor(
    private val activity: Activity,
    private val launcher: ActivityResultLauncher<App>,
    private val iconManager: IIconManager,
    private var list: List<IApp>
) : RecyclerView.Adapter<AppsListAdapter.AbstractViewHolder>() {

    /**
     * Lista filtrada.
     * */
    private var finalList: MutableList<IApp> = list.toMutableList()

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
                    AppItemBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            APP_GROUP_HOLDER_TYPE -> {
                AppGroupViewHolder(
                    AppGroupItemBinding.inflate(
                        inflater,
                        parent,
                        false
                    )
                )
            }
            else -> {
                HeaderViewHolder(
                    HeaderItemBinding.inflate(
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

    override fun getItemCount() = finalList.size

    /**
     * Filtra la lista usando el nombre de las aplicaciones.
     * Actualiza la lista con los resultados de la búsqueda.
     *
     * @param query - Texto que debe contener el nombre de la aplicación a buscar.
     * */
    fun filter(query: String?) {
        finalList = if (query != null && query.isNotBlank()) {
            list.where { it.name.contains(query, true) }.toMutableList()
        } else {
            list.toMutableList()
        }
        notifyDataSetChanged()
    }

    /**
     * Actualiza la lista de aplicaciones y refresca el RecyclerView.
     *
     * @param list - Lista de aplicaciones.
     * */
    fun updateList(list: List<IApp>) {
        this.list = list
        this.finalList = list.toMutableList()
        expandedList.clear()

        notifyDataSetChanged()
    }

    fun updateApp(app: IApp) {
        val index = finalList.indexOf(app)

        if (index != -1) {
            finalList[index] = app
            notifyItemChanged(index)
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
    private inline fun List<IApp>.where(predicate: (IApp) -> Boolean): List<IApp> {
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
        private val binding: HeaderItemBinding
    ) : AbstractViewHolder(binding.root) {

        override fun bind(app: IApp) {
            binding.headerText.text = app.name
        }
    }

    /**
     * Procesa una instancia de [App].
     * */
    private inner class AppViewHolder(
        private val binding: AppItemBinding
    ) : AbstractViewHolder(binding.root) {

        override fun bind(app: IApp) {
            //Casteo al tipo App. Si no es este tipo se lanza una excepción
            app as App

            binding.app = app

            binding.backgroundLayout.setOnClickListener {

                val activityOptions = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    Pair(binding.backgroundLayout, VIEW_NAME_HEADER_LAYOUT),
                    Pair(binding.icon, VIEW_NAME_HEADER_IMAGE),
                    Pair(binding.name, VIEW_NAME_HEADER_NAME),
                    Pair(binding.packageName, VIEW_NAME_HEADER_PACKAGE_NAME)
                )

                launcher.launch(app, activityOptions)
            }

            //Carga el ícono asíncronamente
            iconManager.getAsync(app.packageName, app.version, iconSize) {
                binding.icon.setImageBitmap(it)
            }
        }
    }

    /**
     * Procesa una instancia de [AppGroup].
     * */
    private inner class AppGroupViewHolder(
        private val binding: AppGroupItemBinding
    ) : AbstractViewHolder(binding.root) {

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

            binding.childLayout.visibility = if (app.expanded) View.VISIBLE else View.GONE

            binding.backgroundLayout.setOnClickListener {
                app.expanded = !app.expanded
                notifyItemChanged(adapterPosition)
            }

            childAdapter = AppsListAdapter(
                activity,
                launcher,
                iconManager,
                app
            ).apply {
                iconSize = 40
            }

            binding.child.adapter = childAdapter

            if (app.expanded) {
                binding.arrow.rotation = 180F
            } else {
                binding.arrow.rotation = 0F
            }

            iconManager.getAsync(app.packageName, iconSize) {
                binding.icon.setImageBitmap(it)
            }
        }
    }

    abstract inner class AbstractViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        abstract fun bind(app: IApp)
    }
}