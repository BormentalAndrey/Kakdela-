// Сохранить в app/src/main/java/com/vasilisina/azbuka/ui/fairytale/FairyTaleScreen.kt

package com.vasilisina.azbuka.ui.fairytale

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vasilisina.azbuka.audio.AudioPlayer
import com.vasilisina.azbuka.ui.theme.*

private const val TALE_TITLE = "Волшебный клубочек"
private const val TALE_SUBTITLE = "Путешествие по матушке-России"

private val fairyTaleText = """
Глава 1. Волшебная находка

В сарафане Василиса,
Ленты в две косы вплела.
Ей семь лет, она учиться
Любит, добрые дела.
Кузе в синей рубашонке
Лишь четыре, зуб пропал.
Хоть боится он в сторонке,
Но учиться возмечтал.
В мамин шкаф они взглянули:
Там клубок, а с ним тетрадь.
«С буквы А начнём, мамуля,
Край российский узнавать!»

Глава 2. Древний Новгород

Мчит клубочек по дорожке,
В старый город нас привёл.
Кузя прячется немножко,
Но кремлёвский двор нашёл.
Буква Н — начало сказки,
Древний Новгород стоит.
Край российский дарит краски,
Не боится он обид!
Деда вспомнили с улыбкой,
Смотрим мы на этот град.
Кузя грамоте учился,
Букве Н он очень рад!

Глава 3. Здравствуй, Москва!

А клубочек по тропинке
Прямо к площади спешит.
Красная! Как на картинке,
Папа нам её хранит.
Кремль стоит, он всем известный,
Буква М — Москва зовёт.
Край российский, край прелестный,
Нас с тобой всё дальше ждёт.
Кузя шепчет: «Ты же знаешь,
С папой видели всё мы.
Край родной не променяешь,
Лучше нет родной страны!»

Глава 4. Богатырская застава

Мы пришли, стоит застава,
Там Илья в своей броне.
В нём живёт былая слава,
Служит он родной стране.
«Буква Б — Боец отважный!» —
Молвил Муромец седой.
Кузя шепчет: «В день столь важный
Прадед мой — большой герой!»
Землю русскую и брата
Охранять учитесь вы.
Это главная награда,
Радость нашей детворы!

Глава 5. Золотое кольцо

Вот Кольцо горит златое,
Солнце светит над землёй.
Время бабушки святое,
Край российский и родной!
А матрёшки встали в ряд,
Буква У — Узор приятный!
«Всё по-русски!» — говорят,
Край большой и необъятный.
Краски города полны,
Радуются наши глазки.
С Родиной мы сплетены,
Словно в доброй старой сказке!

Глава 6. Лесная считалочка

Мы зашли в дремучий лес,
Филин мудрый прилетел.
В крае множества чудес
Он на веточку присел:
«Раз, два, три — считаем звёзды
В ясном небе над землёй.
В дом пора идти, уж поздно,
Мама ждёт вас, брат с сестрой!»
Над родной страной великой
Звёзды ясные горят.
Счёт — помощник многоликий,
С братом учим всё подряд!

Глава 7. Чудо-Байкал

Озеро Байкал сияет,
Буква О над ним плывёт.
Край российский вдохновляет,
Красотой своей зовёт.
Омуль машет плавниками
Для ликующих детей.
Воды чистые пред нами,
Нет прекрасней и светлей!
Дядя наш — рыбак умелый,
Вспомнил Кузя-малышок.
Сохранять природу смело
Учит сказочный клубок!

Глава 8. Самая большая страна

Повернул клубочек нить,
Достает сестрица карту.
Чтоб Россию изучить,
Нужно больше, чем за партой!
Смотрит Кузя — вот так да!
В мире нет страны крупнее.
От границы никуда
Не уйдём, она роднее!
Там, на карте, дом родной,
Где нас ждёт любимый дед.
Жить в России нам с тобой —
Это счастье, спору нет!

Глава 9. Семейный ужин

Возвратились наконец!
Пироги печёт бабуля.
Рядом мама и отец,
И сестрёнка, и дедуля.
Кузя встал, забыв про страх:
«Цифра Семь — Семья родная!»
В этих ласковых стенах
Живы мы, беды не зная.
Родину нельзя предать,
Как и тех, кто с нами рядом.
Будем край свой прославлять,
Согревать семью всю взглядом!

Глава 10. До новых встреч!

Сказка близится к концу,
Улыбается читатель.
Подошло добро к лицу,
Учит Родине мечтатель.
Буквы, цифры изучив,
Кузя стал совсем отважным.
Русский искренний мотив
Остаётся в сердце каждом.
Берегите отчий край,
Маму, папу берегите!
Ты, Россия, процветай,
Свет семьи в себе храните!

Уважаемые родители!
Эта лёгкая стихотворная сказка создана специально для совместного чтения с детьми 5–7 лет. Ритм хорея и ямба помогает ребёнку легко воспринимать текст на слух, а небольшие главы не дают заскучать.

Чему учит эта сказка:

Любви к Родине: путешествуя по значимым местам России (Новгород, Москва, Золотое кольцо, Байкал), дети знакомятся с масштабами и красотой родной страны.

Семейным ценностям: в каждой главе герои с теплом вспоминают своих близких — маму, папу, дедушку, бабушку, братьев и сестёр, подчеркивая, что семья — это главная опора.

Преодолению страхов: на примере маленького Кузи ребёнок видит, что любознательность и поддержка друзей помогают стать смелее.

Основам обучения: органично вплетенные буквы (А, Н, М, Б, У, О) и цифры закрепляют базовые знания дошкольников в игровой форме.

Приятного вам чтения и новых открытий вместе с вашими малышами!


""".trimIndent()

@Composable
fun FairyTaleScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WhiteBackground)
            .systemBarsPadding()
            .navigationBarsPadding()
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = TALE_TITLE, style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold), color = FairyPurple, textAlign = TextAlign.Center)
        Text(text = TALE_SUBTITLE, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp), color = DarkText, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(10.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState).fillMaxWidth().background(FairyPink.copy(alpha = 0.08f), RoundedCornerShape(10.dp)).padding(14.dp)) {
            Text(text = fairyTaleText, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp), color = DarkText, textAlign = TextAlign.Start)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(onClick = { AudioPlayer.playSFX("click"); onBack() }, modifier = Modifier.fillMaxWidth(0.5f).height(42.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = FairyPurple, contentColor = Color.White)) {
            Text("Назад", style = MaterialTheme.typography.labelLarge)
        }
    }
}
