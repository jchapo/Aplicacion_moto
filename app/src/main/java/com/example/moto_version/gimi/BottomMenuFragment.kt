import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.example.moto_version.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel

class BottomMenuFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Aplicar esquinas redondeadas al bottom sheet
        val bottomSheet = view.parent as View
        val shapeAppearanceModel = ShapeAppearanceModel.builder()
            .setTopLeftCornerSize(28f)
            .setTopRightCornerSize(28f)
            .build()
        val shapeDrawable = MaterialShapeDrawable(shapeAppearanceModel)
        shapeDrawable.fillColor = ContextCompat.getColorStateList(requireContext(), R.color.md_theme_surface)
        bottomSheet.background = shapeDrawable

        view.findViewById<MaterialButton>(R.id.btnPedidos).setOnClickListener {
            // Utilizar SnackBar en lugar de Toast para mejor integración con Material Design 3
            com.google.android.material.snackbar.Snackbar.make(
                requireView(),
                "Pedidos seleccionado",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnProveedores).setOnClickListener {
            com.google.android.material.snackbar.Snackbar.make(
                requireView(),
                "Proveedores seleccionado",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnMotorizados).setOnClickListener {
            com.google.android.material.snackbar.Snackbar.make(
                requireView(),
                "Motorizados seleccionado",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnCerrarSesion).setOnClickListener {
            com.google.android.material.snackbar.Snackbar.make(
                requireView(),
                "Sesión cerrada",
                com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
            ).show()
            dismiss()
        }
    }
}