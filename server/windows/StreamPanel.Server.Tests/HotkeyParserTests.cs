using StreamPanel.Server.Windows;

namespace StreamPanel.Server.Tests;

public class HotkeyParserTests
{
    [Fact]
    public void Parse_KeepsTheOrderOfTheKeys()
    {
        var keys = HotkeyParser.Parse("CTRL+SHIFT+ESC");

        Assert.Equal<ushort>([0x11, 0x10, 0x1B], keys.Select(k => k.VirtualKey));
    }

    [Theory]
    [InlineData("a", 'A')]
    [InlineData("z", 'Z')]
    [InlineData("5", '5')]
    public void LettersAndDigits_UseTheirCharacterCode(string token, char expected) =>
        Assert.Equal((ushort)expected, HotkeyParser.Parse(token).Single().VirtualKey);

    [Theory]
    [InlineData("F1", 0x70)]
    [InlineData("F4", 0x73)]
    [InlineData("f12", 0x7B)]
    [InlineData("F24", 0x87)]
    public void FunctionKeys(string token, int expected) =>
        Assert.Equal((ushort)expected, HotkeyParser.Parse(token).Single().VirtualKey);

    [Theory]
    [InlineData("UP")]
    [InlineData("DOWN")]
    [InlineData("LEFT")]
    [InlineData("RIGHT")]
    [InlineData("DELETE")]
    [InlineData("HOME")]
    [InlineData("END")]
    [InlineData("PAGEUP")]
    [InlineData("PAGEDOWN")]
    [InlineData("INSERT")]
    public void CursorAndEditingKeys_AreExtended_SoTheyAreNotTakenForTheNumberPad(string token) =>
        Assert.True(HotkeyParser.Parse(token).Single().Extended);

    [Theory]
    [InlineData("CTRL")]
    [InlineData("A")]
    [InlineData("F5")]
    [InlineData("ENTER")]
    public void OrdinaryKeys_AreNotExtended(string token) =>
        Assert.False(HotkeyParser.Parse(token).Single().Extended);

    [Fact]
    public void WinArrow_IsTheWindowSnapShortcut()
    {
        var keys = HotkeyParser.Parse("WIN+LEFT");

        Assert.Equal<ushort>([0x5B, 0x25], keys.Select(k => k.VirtualKey));
    }

    [Theory]
    [InlineData("`", 0xC0)]
    [InlineData("-", 0xBD)]
    [InlineData("=", 0xBB)]
    [InlineData("[", 0xDB)]
    [InlineData("/", 0xBF)]
    [InlineData("PLUS", 0xBB)]
    public void Punctuation(string token, int expected) =>
        Assert.Equal((ushort)expected, HotkeyParser.Parse(token).Single().VirtualKey);

    [Fact]
    public void CtrlBacktick_IsTheVsCodeTerminalShortcut() =>
        Assert.Equal<ushort>([0x11, 0xC0], HotkeyParser.Parse("CTRL+`").Select(k => k.VirtualKey));

    [Fact]
    public void Numpad() =>
        Assert.Equal((ushort)0x65, HotkeyParser.Parse("NUMPAD5").Single().VirtualKey);

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("+")]
    public void Empty_IsRejected(string hotkey) =>
        Assert.Throws<InvalidOperationException>(() => HotkeyParser.Parse(hotkey));

    [Theory]
    [InlineData("CTRL+BANANA")]
    [InlineData("F25")]
    [InlineData("F0")]
    public void UnknownKey_NamesTheCulprit(string hotkey) =>
        Assert.Contains("Unsupported hotkey token", Assert.Throws<InvalidOperationException>(() => HotkeyParser.Parse(hotkey)).Message);
}
