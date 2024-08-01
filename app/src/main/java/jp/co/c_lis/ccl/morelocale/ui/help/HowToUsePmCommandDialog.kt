package jp.co.c_lis.ccl.morelocale.ui.help

import android.Manifest
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import jp.co.c_lis.ccl.morelocale.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

class HowToUsePmCommandDialog : AppCompatDialogFragment() {

    companion object {
        val TAG = HowToUsePmCommandDialog::class.simpleName

        private const val INTERVAL_CHECK_PERMISSION = 1000L

        fun getInstance(): HowToUsePmCommandDialog {
            return HowToUsePmCommandDialog()
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.use_pm_command)
            .setMessage(R.string.use_pm_command_message)
            .setPositiveButton(R.string.click_to_copy_the_command) { _, _ ->
                val context = requireContext()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                clipboard?.setPrimaryClip(ClipData.newPlainText("command", "adb shell pm grant ${context.packageName} android.permission.CHANGE_CONFIGURATION"))
                Toast.makeText(context, R.string.already_copy_the_command, Toast.LENGTH_SHORT).show()
            }
            .create()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        startCheckJob()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private var checkPermissionJob: Job? = null

    private fun startCheckJob() {
        checkPermissionJob = lifecycleScope.launchWhenStarted {
            while (true) {
                delay(INTERVAL_CHECK_PERMISSION)

                if (ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.CHANGE_CONFIGURATION
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(requireContext(), R.string.permission_granted, Toast.LENGTH_LONG)
                        .show()
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        checkPermissionJob?.cancel()
    }
}
