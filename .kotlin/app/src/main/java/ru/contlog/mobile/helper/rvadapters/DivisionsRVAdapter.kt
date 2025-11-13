package ru.contlog.mobile.helper.rvadapters


// Импорты стандартных классов Android для работы с RecyclerView
import android.view.LayoutInflater          // Класс для создания View из XML-файлов
import android.view.ViewGroup               // Контейнер, в который помещаются элементы RecyclerView
import androidx.recyclerview.widget.RecyclerView            // Базовый класс адаптера и ViewHolder'а для списков

// Импорт ViewBinding, сгенерированного из layout-файла элемента списка
import ru.contlog.mobile.helper.databinding.ItemDivisionBinding

// Импорт ViewBinding, сгенерированного из layout-файла элемента списка
import ru.contlog.mobile.helper.model.Division


// Адаптер для RecyclerView, отображающий список подразделений.
// Принимает лямбду `onOpen`, вызываемую при нажатии на элемент списка.
class DivisionsRVAdapter(private val onOpen: (Division) -> Unit) : RecyclerView.Adapter<DivisionsRVAdapter.VH>() {

    // Внутренний изменяемый список для хранения данных подразделений
    private val divisionsList = mutableListOf<Division>()

    // Создаёт новый ViewHolder, когда RecyclerView нужно отобразить новый элемент (вне экрана)
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {
        val inflater = LayoutInflater.from(parent.context)      // Получаем LayoutInflater из контекста родительского ViewGroup
        val binding = ItemDivisionBinding.inflate(inflater, parent, false)      // Создаём экземпляр ViewBinding для layout-файла элемента списка

        // Возвращаем новый экземпляр ViewHolder, передавая ему binding и callback
        return VH(binding, onOpen)
    }

    // Привязывает данные к существующему ViewHolder (повторное использование при прокрутке)
    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {
        // Передаём элемент списка из внутреннего хранилища в метод bind ViewHolder'а
        holder.bind(divisionsList[position])
    }

    // Возвращает общее количество элементов в списке (для отображения в RecyclerView)
    override fun getItemCount(): Int = divisionsList.size

    // Метод для обновления данных в адаптере с частичной перерисовкой (с анимацией)
    fun setData(newData: List<Division>) {
        val oldCount = divisionsList.size       // Сохраняем текущее количество элементов для уведомления об удалении
        divisionsList.clear()               // Очищаем текущий список
        notifyItemRangeRemoved(0, oldCount)             // Уведомляем адаптер, что все старые элементы удалены (для корректной анимации)
        divisionsList.addAll(newData)           // Добавляем новые данные
        notifyItemRangeChanged(0, divisionsList.size)       // Уведомляем адаптер, что элементы изменились и нужна перерисовка
    }

    // Внутренний класс ViewHolder — держит ссылки на UI-элементы одного элемента списка
    class VH(private val binding: ItemDivisionBinding, private val onOpen: (Division) -> Unit) : RecyclerView.ViewHolder(binding.root) {

        // Метод для привязки данных модели к UI-элементам
        fun bind(division: Division) {
            binding.name.text = division.name           // Устанавливаем название подразделения в TextView с id "name"
            binding.address.text = division.address     // Устанавливаем адрес подразделения в TextView с id "address"

            // Настраиваем обработчик нажатия на корневой элемент (всю карточку)
            binding.root.setOnClickListener {
                onOpen(division)            // Вызываем переданный callback, передавая выбранное подразделение
            }
        }
    }
}