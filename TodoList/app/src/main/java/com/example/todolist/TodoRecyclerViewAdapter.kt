package com.example.todolist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.todolist.databinding.ItemTodoBinding
import com.example.todolist.db.TodoEntity

class TodoRecyclerViewAdapter(private val todoList : ArrayList<TodoEntity>) : RecyclerView.Adapter<TodoRecyclerViewAdapter.MyViewHolder>() {
    // ViewHolder 패턴 : 각 뷰 객체를 뷰홀더에 보관해서 반복 메서드 호출을 줄여 속도를 개선

    inner class MyViewHolder(binding : ItemTodoBinding) : RecyclerView.ViewHolder(binding.root) {
        val tv_importance = binding.tvImportance
        val tv_title = binding.tvTitle

        val root = binding.root // ConstraintLayout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding : ItemTodoBinding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return todoList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // 뷰홀더 객체에 데이터를 넣어줌 (바인딩)
        val todoData = todoList[position]

        when (todoData.importance) {
            1 -> {
                holder.tv_importance.setBackgroundResource(R.color.red)
            }
            2 -> {
                holder.tv_importance.setBackgroundResource(R.color.yellow)
            }
            3 -> {
                holder.tv_importance.setBackgroundResource(R.color.green)
            }
        }

        holder.tv_importance.text = todoData.importance.toString()
        holder.tv_title.text = todoData.title
    }

}