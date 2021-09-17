package com.smartsolutions.paquetes.ui.firewall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemAppGroupBinding
import com.smartsolutions.paquetes.databinding.ItemAppBinding
import com.smartsolutions.paquetes.databinding.ItemHeaderBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.parcelize.Parcelize

private const val APP_HOLDER_TYPE = 0
private const val APP_GROUP_HOLDER_TYPE = 1
private const val HEADER_HOLDER_TYPE = 2

/**
 * Adaptador del [RecyclerView] del cortafuegos.
 * */
class AppsListAdapter(
    private val context: Context,
    private val list: List<IApp>,
    private val iconManager: IIconManager
) : RecyclerView.Adapter<AppsListAdapter.ViewHolder>() {

    /**
     * Se lanza cuando una aplicacion o un grupo de aplicaciones cambia su acceso.
     * */
    var onAccessChange: ((app: IApp) -> Unit)? = null

    /**
     * Tamaño de los íconos de las aplicaciones.
     * */
    private var iconSize: Int = 150

    /**
     * Lista ordenada y con encabezados.
     * */
    private var finalList = prepareAppList(list)

    /**
     * Mapa que se usa para guardar el estado de los grupos de aplicaciones.
     * Se usa como llave el hashCode del grupo y como valor el estado (true) si está
     * expandido, (false) si está colapsado.
     * */
    private val expandedList = mutableMapOf<Int, Boolean>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(context)

        /*
        * Retorno el ViewHolder correspondiente al tipo de app.
        * */
        return when(viewType) {
            APP_HOLDER_TYPE -> {
                AppViewHolder(ItemAppBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ))
            }
            APP_GROUP_HOLDER_TYPE -> {
                AppGroupViewHolder(
                    ItemAppGroupBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )
            }
            else -> {
                HeaderViewHolder(ItemHeaderBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(finalList[position])
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttached()
    }

    override fun getItemCount() = finalList.size

    override fun getItemViewType(position: Int): Int {
        return when {
            finalList[position] is App -> APP_HOLDER_TYPE
            finalList[position] is AppGroup -> APP_GROUP_HOLDER_TYPE
            else -> HEADER_HOLDER_TYPE
        }
    }

    /**
     * Filtra la lista usando el nombre de las aplicaciones.
     * Actualiza la lista con los resultados de la búsqueda.
     *
     * @param query - Texto que debe contener el nombre de la aplicación a buscar.
     * */
    fun filter(query: String?) {
        finalList = if (query != null && query.isNotBlank())
            prepareAppList(list.where { it.name.contains(query, true) })
        else
            prepareAppList(list)

        notifyDataSetChanged()
    }

    /**
     * Ordena la lista de aplicaciones y le agrega encabezados.
     * */
    private fun prepareAppList(apps: List<IApp>): List<IApp> {
        //Resultado final
        val result = mutableListOf<IApp>()

        //Aplicaciones permitidas
        val allowedApps = apps.filter { it.access }
        //Aplicaciones bloqueadas
        val blockedApps = apps.filter { !it.access }

        /*Indica si se deben agregar encabezados. Si una de las listas de aplicaciones
        * está vacia, no se agregan encabezados.*/
        val addHeaders = allowedApps.isNotEmpty() && blockedApps.isNotEmpty()

        if (addHeaders) {
            //Encabezado de aplicaciones permitidas.
            result.add(
                HeaderApp(
                    "",
                    -1,
                    context.getString(R.string.allowed_apps, allowedApps.size),
                    access = false,
                    system = false,
                    null,
                    null
                )
            )
        }

        result.addAll(allowedApps)

        if (addHeaders) {
            //Encabezado de aplicaciones bloqueadas
            result.add(
                HeaderApp(
                    "",
                    -1,
                    context.getString(R.string.blocked_apps, blockedApps.size),
                    access = false,
                    system = false,
                    null,
                    null
                )
            )
        }
        result.addAll(blockedApps)

        return result
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
     * ViewHolder que procesa una instancia de [App].
     * */
    private inner class AppViewHolder(
        private val binding: ItemAppBinding,
    ): ViewHolder(binding.root) {

        /**
         * Nombre de paquete que se usa para cargar el ícono
         * en el método [onAttached]
         * */
        private var packageName: String? = null

        /**
         * Versión que se usa para cargar el ícono
         * en el método [onAttached]
         * */
        private var version: Long? = null

        override fun bind(app: IApp) {
            //Casteo al tipo App. Si no es este tipo se lanza una excepción
            app as App

            binding.app = app
            packageName = app.packageName
            version = app.version

            //setCheckBoxListener(app, binding.access)

            binding.backgroundLayout.setOnClickListener {
                //TODO: Abrir una actividad con los detalles de la app.
                Toast.makeText(context, "Back clicked", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onAttached() {
            /*Este método se lanza cuando la view se adjunta a la ventana. Entonces
            * se carga el ícono.*/
            packageName?.let { packageName ->
                version?.let { version ->
                    iconManager.getAsync(packageName, version, iconSize) {
                        binding.icon.setImageBitmap(it)
                    }
                }
            }
        }
    }

    /**
     * ViewHolder que procesa una instancia de [AppGroup]
     * */
    private inner class AppGroupViewHolder(
        private val binding: ItemAppGroupBinding
    ) : ViewHolder(binding.root) {

        /**
         * Nombre de paquete que se usa para cargar el ícono
         * en el método [onAttached]
         * */
        private var packageName: String? = null

        /**
         * Adaptador de la lista de aplicaciones anidada.
         * */
        private lateinit var childAdapter: AppsListAdapter

        override fun bind(app: IApp) {
            //Casteo al tipo AppGroup. Si no es este tipo se lanza una excepción
            app as AppGroup
            packageName = app.packageName

            binding.name.text = app.name
            binding.appsCount.text = context.getString(R.string.apps_count, app.size)

            //setCheckBoxListener(app, binding.access)

            binding.childLayout.visibility = if (app.expanded) View.VISIBLE else View.GONE

            binding.backgroundLayout.setOnClickListener {
                app.expanded = !app.expanded
                notifyItemChanged(adapterPosition)
            }

            childAdapter = AppsListAdapter(
                context,
                app,
                iconManager
            ).apply {
                iconSize = 110
                onAccessChange = {
                    this@AppsListAdapter.notifyItemChanged(adapterPosition)
                    this@AppsListAdapter.onAccessChange?.invoke(it)
                }
            }

            binding.child.adapter = childAdapter

            if (app.expanded) {
                binding.arrow.rotation = 180F
            } else {
                binding.arrow.rotation = 0F
            }
        }

        override fun onAttached() {
            /*Este método se lanza cuando la view se adjunta a la ventana. Entonces
             * se carga el ícono.*/
            packageName?.let { packageName ->
                iconManager.getAsync(packageName, iconSize) { img ->
                    binding.icon.setImageBitmap(img)
                }
            }
        }

        override fun onAccessChange(app: IApp) {
            super.onAccessChange(app)
            /* Notifico en el adaptador anidado que la lista
             * de aplicaciones anidadas cambiaron*/
            notifyItemChanged(adapterPosition)
        }
    }

    /**
     * ViewHolder que procesa una instancia de [HeaderApp].
     * Solo dibuja un encabezado con el texto que contiene el HeaderApp en
     * la propiedad name.
     * */
    private inner class HeaderViewHolder(
        private val binding: ItemHeaderBinding
    ) : ViewHolder(binding.root) {

        override fun bind(app: IApp) {
            binding.headerText.text = app.name
        }
    }

    abstract inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        /**
         * Se llama para conectar la vista a los datos y asignar los eventos.
         * */
        abstract fun bind(app: IApp)

        /**
         * Se llama cuando se adjunta la vista a la ventana.
         * */
        open fun onAttached() {
            //Empty
        }

        /**
         * Asigna el evento onCheckedChange al checkBox y establece la propiedad
         * [CompoundButton.isChecked] de manera segura sin lanzar el evento
         * accidentalmente.
         * */
        protected fun setCheckBoxListener(app: IApp, checkBox: CompoundButton) {
            checkBox.setOnCheckedChangeListener(null)
            checkBox.isChecked = app.access
            checkBox.setOnCheckedChangeListener { _,_ ->
                handleWarningMessages(app, checkBox)
            }
        }

        /**
         * Maneja los mensajes de advertencia si los hay y cambia el acceso a la app.
         * */
        private fun handleWarningMessages(app: IApp, checkBox: CompoundButton) {

            /*Diálogo que se mostrará cuando exista un mensaje de advertencia.*/
            val dialog = AlertDialog.Builder(itemView.context)
                .setTitle(R.string.warning_title)
                .setNegativeButton(R.string.btn_cancel
                ) { _,_ ->
                    /*Si se oprime el botón cancelar se llama al método
                    * setCheckBoxListener para restablecer el estado anterior del checkBox si
                    * lanzar el evento de este. Este método utiliza la propiedad access de la app,
                    * que no se ha cambiado todavía.*/
                    setCheckBoxListener(app, checkBox)
                }
                .setPositiveButton(R.string.btn_continue) { _,_ ->
                    app.access = checkBox.isChecked
                    onAccessChange(app)
                }

            if (checkBox.isChecked && app.allowAnnotations != null) {
                dialog.setMessage(app.allowAnnotations)
                    .show()
            } else if (!checkBox.isChecked && app.blockedAnnotations != null) {
                dialog.setMessage(app.blockedAnnotations)
                    .show()
            } else {
                /*Si no hay ningún mensaje de advertencia cambio la propiedad access e invoco
                * al método onAccessChange*/
                app.access = checkBox.isChecked
                onAccessChange(app)
            }
        }

        /**
         * Se llama cuando cambia el acceso de una app. Este método
         * llama a la propiedad [onAccessChange] del adaptador por
         * lo que no se debe volver a llamar a dicha propiedad.
         *
         * @param app - IApp que se le cambió el acceso.
         * */
        protected open fun onAccessChange(app: IApp) {
            onAccessChange?.invoke(app)
        }
    }

    /**
     * Modelo que se usa para dibujar un encabezado en la lista. Solo
     * contiene el nombre del encabezado en la propiedad name. Las demas
     * propiedades están vacias o nulas. El método [accessHashCode] no está
     * soportado.
     * */
    @Parcelize
    private class HeaderApp(
        override var packageName: String,
        override var uid: Int,
        override var name: String,
        override var access: Boolean,
        override var system: Boolean,
        override var allowAnnotations: String?,
        override var blockedAnnotations: String?
    ) : IApp {

        override fun accessHashCode(): Long {
            throw UnsupportedOperationException()
        }
    }
}