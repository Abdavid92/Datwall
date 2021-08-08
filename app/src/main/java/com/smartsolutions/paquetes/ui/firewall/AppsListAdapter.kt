package com.smartsolutions.paquetes.ui.firewall

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.diegodobelo.expandingview.ExpandingItem
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.AnimatedAppGroupItemBinding
import com.smartsolutions.paquetes.databinding.AppGroupItemBinding
import com.smartsolutions.paquetes.databinding.AppItemBinding
import com.smartsolutions.paquetes.databinding.HeaderItemBinding
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
    list: List<IApp>,
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
    private val list = prepareAppList(context, list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)

        /*
        * Retorno el ViewHolder correspondiente al tipo de app.
        * */
        return when(viewType) {
            APP_HOLDER_TYPE -> {
                AppViewHolder(AppItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ))
            }
            APP_GROUP_HOLDER_TYPE -> {
                AnimatedAppGroupViewHolder(
                    layoutInflater.inflate(
                        R.layout.animated_app_group_item,
                        parent,
                        false
                    )
                )
                /*AppGroupViewHolder(AppGroupItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ))*/
            }
            else -> {
                HeaderViewHolder(HeaderItemBinding.inflate(
                    layoutInflater,
                    parent,
                    false
                ))
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        holder.onAttached()
    }

    override fun getItemCount() = list.size

    override fun getItemViewType(position: Int): Int {
        return when {
            list[position] is App -> APP_HOLDER_TYPE
            list[position] is AppGroup -> APP_GROUP_HOLDER_TYPE
            else -> HEADER_HOLDER_TYPE
        }
    }

    /**
     * Ordena la lista de aplicaciones y le agrega encabezados.
     * */
    private fun prepareAppList(context: Context, apps: List<IApp>): List<IApp> {
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
                    false,
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
                    false,
                    null,
                    null
                )
            )
        }
        result.addAll(blockedApps)

        return result
    }

    /**
     * ViewHolder que procesa una instancia de [App].
     * */
    private inner class AppViewHolder(
        private val binding: AppItemBinding,
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

            setCheckBoxListener(app, binding.access)

            binding.backLayout.setOnClickListener {
                //Temp: Abrir una actividad con los detalles de la app.
                Toast.makeText(itemView.context, "Back clicked", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onAttached() {
            /*Este método se lanza cuando la view se adjunta a la ventana. Entonces
            * se carga el ícono.*/
            packageName?.let { packageName ->
                version?.let { version ->
                    iconManager.getAsync(packageName, version) {
                        Glide.with(itemView)
                            .asBitmap()
                            .load(it)
                            .override(iconSize)
                            .into(binding.icon)
                    }
                }
            }
        }
    }

    /**
     * ViewHolder que procesa una instancia de [AppGroup].
     * */
    private inner class AppGroupViewHolder(
        private val binding: AppGroupItemBinding
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

            binding.name.text = app.name
            binding.appsCount.text = context.getString(R.string.apps_count, app.size)
            packageName = app.packageName

            setCheckBoxListener(app, binding.access)

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

            /*Evento click en el layout de la vista para abrir o cerrar la lista
            * anidada. Me guio por el estado de visibilidad del layout y aplico
            * una animación que está sujeta a cambios.*/
            binding.backLayout.setOnClickListener {
                if (binding.childLayout.visibility == View.GONE) {
                    /*val animation = AnimationUtils
                        .loadAnimation(itemView.context, R.anim.slide_down)*/


                    //binding.childLayout.startAnimation(animation)
                    binding.arrow.animate()
                        .rotation(180F)
                        .duration = 200

                    binding.childLayout.visibility = View.VISIBLE
                } else {
                    /*val animation = AnimationUtils
                        .loadAnimation(itemView.context, R.anim.slide_up)
                        .apply {
                            setAnimationListener(object : Animation.AnimationListener {
                                override fun onAnimationStart(animation: Animation?) {

                                }

                                override fun onAnimationEnd(animation: Animation?) {
                                    binding.childLayout.visibility = View.GONE
                                }

                                override fun onAnimationRepeat(animation: Animation?) {

                                }
                            })
                        }

                    binding.childLayout.startAnimation(animation)*/
                    binding.arrow.animate()
                        .rotation(0F)
                        .duration = 200

                    binding.childLayout.visibility = View.GONE
                }
            }
        }

        override fun onAttached() {
            /*Este método se lanza cuando la view se adjunta a la ventana. Entonces
             * se carga el ícono.*/
            packageName?.let { packageName ->
                iconManager.getAsync(packageName) { img ->
                    Glide.with(itemView)
                        .asBitmap()
                        .load(img)
                        .override(iconSize)
                        .into(binding.icon)
                }
            }
        }

        override fun onAccessChange(app: IApp) {
            super.onAccessChange(app)
            /*Notifico en el adaptador anidado que la lista
             * de aplicaciones anidadas cambiaron*/
            //childAdapter.notifyDataSetChanged()
            notifyItemChanged(adapterPosition)
        }
    }

    private inner class AnimatedAppGroupViewHolder(
        view: View
    ) : ViewHolder(view) {

        /**
         * Nombre de paquete que se usa para cargar el ícono
         * en el método [onAttached]
         * */
        private var packageName: String? = null

        override fun bind(app: IApp) {
            //Casteo al tipo AppGroup. Si no es este tipo se lanza una excepción
            app as AppGroup
            packageName = app.packageName

            itemView.findViewById<TextView>(R.id.name)
                .text = app.name
            itemView.findViewById<TextView>(R.id.apps_count)
                .text = context.getString(R.string.apps_count, app.size)

            setCheckBoxListener(app, itemView.findViewById<CheckBox>(R.id.access))

            fillSubList(app)
        }

        private fun fillSubList(app: AppGroup) {
            val expandingItem = itemView as ExpandingItem

            app.forEach { childApp ->
                expandingItem.createSubItem()?.let { view ->
                    iconManager.getAsync(childApp.packageName, childApp.version) {
                        Glide.with(itemView)
                            .asBitmap()
                            .load(it)
                            .override(iconSize - 40)
                            .into(view.findViewById(R.id.icon))
                    }
                }
            }
        }

        override fun onAttached() {
            /*Este método se lanza cuando la view se adjunta a la ventana. Entonces
             * se carga el ícono.*/
            packageName?.let { packageName ->
                iconManager.getAsync(packageName) { img ->
                    Glide.with(itemView)
                        .asBitmap()
                        .load(img)
                        .override(iconSize)
                        .into(itemView.findViewById(R.id.icon))
                }
            }
        }

    }

    /**
     * ViewHolder que procesa una instancia de [HeaderApp].
     * Solo dibuja un encabezado con el texto que contiene el HeaderApp en
     * la propiedad name.
     * */
    private inner class HeaderViewHolder(
        private val binding: HeaderItemBinding
    ) : ViewHolder(binding.root) {

        override fun bind(app: IApp) {
            binding.headerText.text = app.name
        }

        override fun onAttached() {
            //Empty
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
        abstract fun onAttached()

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

            val dialog = AlertDialog.Builder(itemView.context)
                .setTitle(R.string.warning_title)
                .setNegativeButton(R.string.btn_cancel
                ) { dialog, which ->
                    setCheckBoxListener(app, checkBox)
                }
                .setPositiveButton(R.string.btn_continue) { dialog, which ->
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
                app.access = checkBox.isChecked
                onAccessChange(app)
            }
        }

        /**
         * Se llama cuando cambia el acceso de una app.
         * */
        protected open fun onAccessChange(app: IApp) {
            onAccessChange?.invoke(app)
        }
    }

    /**
     * Modelo que se usa para dibujar un encabezado en la lista.
     * */
    @Parcelize
    private class HeaderApp(
        override var packageName: String,
        override var uid: Int,
        override var name: String,
        override var access: Boolean,
        override var allowAnnotations: String?,
        override var blockedAnnotations: String?
    ) : IApp {

        override fun accessHashCode(): Long {
            throw UnsupportedOperationException()
        }
    }
}