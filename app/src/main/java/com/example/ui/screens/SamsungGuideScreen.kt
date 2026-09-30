package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBlue
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberTeal
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun SamsungGuideScreen() {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Intro Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Посібник для Samsung Galaxy A24",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Як повноцінно користуватися пристроєм без тачскріну",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Step 1: Initial Setup without touch
        GuideCard(
            stepNumber = "1",
            title = "Первинне надання дозволів (OTG-мишка)",
            icon = Icons.Default.Mouse,
            accentColor = CyberCyan
        ) {
            Text(
                text = "Оскільки тачскрін на вашому Samsung A24 зламано, Android вимагає один раз надати системні дозволи (Камера, Оверлей, Доступність):\n\n" +
                        "• Візьміть звичайну комп'ютерну USB-мишку (дротову або бездротову) та дешевий OTG-адаптер (Type-C на USB).\n" +
                        "• Підключіть мишку до зарядного гнізда Samsung A24. На екрані одразу з'явиться стандартна стрілка миші!\n" +
                        "• Натисніть кнопки надання дозволів на вкладці «Головна».\n" +
                        "• Після цього ви зможете відключити мишку й повністю керувати телефоном очима!",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }

        // Step 2: Physical Volume Buttons
        GuideCard(
            stepNumber = "2",
            title = "Фізичні кнопки гучності як клікер",
            icon = Icons.Default.VolumeUp,
            accentColor = WarningAmber
        ) {
            Text(
                text = "На правому ребрі Samsung A24 є кнопки регулювання гучності. Ми перетворили їх на ваш фізичний маніпулятор:\n\n" +
                        "• Гучність ВГОРУ (+) = Натискання (клік) на будь-яку кнопку або пункт на екрані, на який зараз дивиться ваш курсор.\n" +
                        "• Гучність ВНИЗ (-) = Миттєве калібрування центру (якщо ви лягли або пересіли, просто подивіться в центр екрана й натисніть мінус).\n" +
                        "• Також доступний повністю безконтактний клік затримкою погляду (dwell) або підморгуванням!",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }

        // Step 3: Navigation and Scrolling
        GuideCard(
            stepNumber = "3",
            title = "Навігація One UI (Назад, Додому, Скрол)",
            icon = Icons.Default.Navigation,
            accentColor = CyberTeal
        ) {
            Text(
                text = "У верхній частині екрана поверх усіх додатків відображається плаваюча панель дій EyeCursor:\n\n" +
                        "• «Назад» — повернення до попередньої сторінки або закриття вікна.\n" +
                        "• «Дім» — вихід на робочий стіл Samsung One UI.\n" +
                        "• «Меню» — список останніх запущених додатків.\n" +
                        "• «Скрол Вниз / Вгору» — перемикає режим: тепер затримка погляду гортає стрічку новин, Telegram, соцмережі або сайти замість звичайного кліку.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }

        // Step 4: Reboot Safety & Battery
        GuideCard(
            stepNumber = "4",
            title = "Автозапуск та Super AMOLED екран",
            icon = Icons.Default.BatteryChargingFull,
            accentColor = NeonGreen
        ) {
            Text(
                text = "• Samsung A24 оснащений енергоефективним Super AMOLED дисплеєм. Темна тема EyeCursor суттєво економить заряд батареї.\n" +
                        "• Ми увімкнули автозапуск при перезавантаженні: якщо телефон розрядиться або перезавантажиться, EyeCursor автоматично запуститься на фоні, і ви не втратите доступ до пристрою.\n" +
                        "• Рекомендація: встановіть телефон на зручну підставку на столі перед собою на відстані 30–50 см для максимальної точності трекінгу очей.",
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun GuideCard(
    stepNumber: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        color = Color.Black,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            content()
        }
    }
}
