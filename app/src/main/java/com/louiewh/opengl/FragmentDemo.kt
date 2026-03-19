package com.louiewh.opengl

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.PopupWindow
import androidx.fragment.app.Fragment
import com.louiewh.opengl.databinding.FragmentDemoBinding
import com.louiewh.opengl.render.GlesRender

class FragmentDemo : Fragment() {

    private var _binding: FragmentDemoBinding? = null
    private val binding get() = _binding!!

    private var mGlesRender: GlesRender? = null
    private var mCurrentShaderPosition = 0
    private var popupWindow: PopupWindow? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDemoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        initGlesSurfaceView()
    }

    private fun setupSpinner() {
        // 显示当前选中的shader名称
        updateSpinnerText()

        binding.spinnerShader.setOnClickListener {
            showDropdown()
        }
    }

    private fun updateSpinnerText() {
        val shaderName = GlesConst.shaderArray[mCurrentShaderPosition].renderName
        binding.spinnerShader.text = shaderName
    }

    private fun showDropdown() {
        if (popupWindow?.isShowing == true) {
            popupWindow?.dismiss()
            return
        }

        val shaderNames = GlesConst.shaderArray.map { it.renderName }
        val listView = ListView(requireContext())
        listView.setBackgroundColor(0xFFFFFFFF.toInt()) // 白色背景

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, shaderNames)
        listView.adapter = adapter

        // 设置选中项高亮
        listView.setItemChecked(mCurrentShaderPosition, true)
        listView.setSelection(mCurrentShaderPosition)

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position != mCurrentShaderPosition) {
                mCurrentShaderPosition = position
                switchShader(position)
            }
            popupWindow?.dismiss()
        }

        // 计算280dp宽度，200dp高度
        val density = resources.displayMetrics.density
        val widthPx = (280 * density).toInt()
        val heightPx = (200 * density).toInt()

        popupWindow = PopupWindow(listView, widthPx, heightPx, true)
        popupWindow?.isOutsideTouchable = true
        popupWindow?.setBackgroundDrawable(null) // 移除默认背景
        popupWindow?.showAsDropDown(binding.spinnerShader)
    }

    private fun initGlesSurfaceView() {
        val firstShaderName = GlesConst.shaderArray[0].renderName
        Log.e("FragmentDemo", "initGlesSurfaceView with $firstShaderName")

        mGlesRender = GlesRender(firstShaderName).apply {
            setGlesSurfaceView(binding.glesSurfaceView)
        }
    }

    private fun switchShader(position: Int) {
        val shaderName = GlesConst.shaderArray[position].renderName
        Log.e("FragmentDemo", "switchShader to $shaderName")
        updateSpinnerText()

        mGlesRender?.switchShader(shaderName)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        popupWindow?.dismiss()
        popupWindow = null
        mGlesRender?.destroyShader()
        mGlesRender = null
        _binding = null
    }
}