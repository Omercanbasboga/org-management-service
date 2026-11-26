package dev.omercanbasboga.orgservice.bylaws;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BylawsNamesTest {

    @Test
    void displayNameKeepsOnlyTheLastSegment() {
        assertThat(BylawsService.displayName("../../etc/passwd")).isEqualTo("passwd");
        assertThat(BylawsService.displayName("C:\\Users\\x\\tüzük 2026.pdf")).isEqualTo("tüzük 2026.pdf");
        assertThat(BylawsService.displayName("a\u0000b.pdf")).isEqualTo("ab.pdf");
        assertThat(BylawsService.displayName("  ")).isEqualTo("bylaws");
    }

    @Test
    void extensionIsLowercasedAndMissingIsEmpty() {
        assertThat(BylawsService.extension("Bylaws.PDF")).isEqualTo("pdf");
        assertThat(BylawsService.extension("archive.tar.gz")).isEqualTo("gz");
        assertThat(BylawsService.extension("README")).isEmpty();
    }
}
